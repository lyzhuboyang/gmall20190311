package com.zby.gmall.gmallcartservice.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zby.gmall.bean.CartInfo;
import com.zby.gmall.bean.SkuInfo;
import com.zby.gmall.config.RedisUtil;
import com.zby.gmall.gmallcartservice.constant.CartConst;
import com.zby.gmall.gmallcartservice.mapper.CartInfoMapper;
import com.zby.gmall.service.CartService;
import com.zby.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Reference
    private ManageService manageService;

    //表示登录时添加购物车
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /**
         *1、判断购物车中是否有该商品
         * select * from cartInfo where userId = ? and skuId = ?
         *2、有：数量相加
         *3、没有：直接添加到数据库
         *4、放入redis
         *mysql与redis如何进行同步？
         *在添加购物车的时候，直接添加到数据库并添加到redis!
         */
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);

        //数据库查询购物车是否有数据
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);//查询出来已经有主键Id

        /**
         * redis使用注意事项
         * 1、获取jedis对象
         * 2、用那种数据类型存储数据key-value
         */
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //定义key user:userId:cart 这个字符串可以定义常量，相关的字符串可以定义成一个类
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        if(cartInfoExist!=null){
            //数据库中数据，数量相加
            //更新数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //实时价格初始化值
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            //更新到数据库
            cartInfoMapper.updateByPrimaryKey(cartInfoExist);
        }else{
            //没有数据，添加都数据库.
            //获取skuInfo信息。添加cartInfo中
            //把商品信息取出来，新增到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1=new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            //添加到数据库
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist = cartInfo1;
            //放入redis
            /**
             * 用那种数据类型
             * hash jedis.hset(key,field,value)
             * key=user:userId:cart
             * field=skuId
             * value=cartInfoValue
             */
        }

        //Redis hash特别适合用于存储对象。
        //放入redis
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));

        //购物车是否有过期时间？
        //设置过期时间？跟用户的过期时间一致
        //如果把购物车每个商品sku设置过期时间，是不合适的，因为每个商品的过期时间不一致的话，会导致用户的体验极差，我添加的商品跑哪了，这网站是不是出bug了
        //获取用户的key user:userId:info
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        //剩余过期时间
        //ttl k1 查看k1还有多少秒过期，-1表示永不过期，-2表示已过期
        Long ttl = jedis.ttl(userKey);
        //赋值给购物车
        //expire k1 10 为键值设置过期时间为10秒
        jedis.expire(cartKey,ttl.intValue());
        //关闭得太早
        jedis.close();
    }


    @Override
    public List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        /**
         1.获取Jedis
         2.从Jedis中获取数据
         3.如果有：将redis数据返回
         4.如果没有：从数据库查询，查询购物车中的实时价格，并放入redis
         */
        //1获取Jedis
        Jedis jedis = redisUtil.getJedis();

        //2.从Jedis中获取数据
        //  jedis.hgetAll()
        //  jedis.hvals()

        // 定义Key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartList = jedis.hvals(cartKey);
        // 从redis中获取user:userId:cart 的数据
        if(cartList!=null&&cartList.size()>0){//redis中有的话
            for (String cartJson : cartList) {
                //cartJson转换为对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                //添加购物车数据
                cartInfoList.add(cartInfo);
            }
            //查询的时候，按照更新的时间倒序！
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else{//redis中没有的话
            // 从数据库中获取数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }

    }

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        //获取数据库中的数据
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        //合并条件 skuId相同的时候合并
        for (CartInfo cartInfoCK : cartListCK) {
            //声明一个boolean类型的变量
            boolean isMatch = false;
            //有相同的数据直接更新到数据库
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if(cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    //数量相加
                    cartInfoDB.setSkuNum(cartInfoCK.getSkuNum()+cartInfoDB.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }
            if(!isMatch){
                //未登录的时候userId为null
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }

        //最后再查询一次更新一次之后，新添加的所有数据
        List<CartInfo> cartInfoList = loadCartCache(userId);
        
        //合并勾选的商品
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartListCK) {
                if(cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    //判断未登录状态为勾选状态！
                    if("1".equals(cartInfoCK.getIsChecked())){
                        cartInfoDB.setIsChecked("1");

                        //京东做法
//                      cartInfoDB.setSkuNum(cartInfoCK.getSkuNum());
//                      cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                        //redis发送消息队列

                        //自动勾选
                        checkCart(cartInfoDB.getSkuId(),cartInfoCK.getIsChecked(),userId);
                    }
                }
            }
        }
        return cartInfoList;
    }

    /**
     *
     * @param skuId
     * @param userKey
     * @param skuNum
     */
    @Override
    public void addToCartRedis(String skuId, String userKey, int skuNum) {
        /**
         1.先获取所有的数据
         2.判断是否有相同的数据
         3.有数量相加
         4.无：直接添加redis
         */
        Jedis jedis = redisUtil.getJedis();
        //定义key  user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userKey+CartConst.USER_CART_KEY_SUFFIX;
        Map<String, String> map = jedis.hgetAll(cartKey);
        String cartInfoJson = map.get(skuId);
        if(StringUtils.isNotEmpty(cartInfoJson)){
            //redis中有，数量相加
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
        }else{
            //没有，直接添加到redis
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuNum(skuNum);
            jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
        }
        jedis.close();
    }

    /**
     * 选中商品
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /**
         * 1.将页面传递过来的商品Id,与购物车中的商品Id进行匹配
         * 2.修改isChecked中的数据
         * 3.单独创建一个key来存储已选中的商品
         */
        //创建Jedis
        Jedis jedis = redisUtil.getJedis();
        //定义user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //获取选中的商品
        String cartInfoJson = jedis.hget(cartKey, skuId);
        //把cartInfoJson转换为对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        //将cartInfo写回购物车
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfo));
        //重新记录被勾选的商品 user:userId:checked
        String cartCheckKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            jedis.hset(cartCheckKey,skuId,JSON.toJSONString(cartInfo));
        }else{
            jedis.hdel(cartCheckKey,skuId);
        }
    }

    /**
     * 根据用户Id查询选中的商品
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //获取Jedis
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartCheckedKey);
        for (String cartInfoJson : stringList) {
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            cartInfoList.add(cartInfo);
        }
        jedis.close();
        return cartInfoList;
    }

    /**
     * 根据userId查询数据并放入缓存
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        /**
         * 1、根据userId查询一下当前商品的实时价格
         * 2、将查询出来的数据放入缓存!
         */
        List<CartInfo> cartInfoList =  cartInfoMapper.selectCartListWithCurPrice(userId);
        /**
         * 面试JavaScript = == ===
         * =赋值  ==比较 ===js中的全等，类型和值必须相等
         * == 基本类型，判断的是值；引用类型，判断的是地址
         */
        if(cartInfoList==null&&cartInfoList.size()==0){
            return null;
        }

        Jedis jedis = redisUtil.getJedis();
        // 定义Key user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

//        for (CartInfo cartInfo : cartInfoList) {
//            // 每次放一条数据
//            jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//        }

        Map<String,String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        //将map放入缓存
        jedis.hmset(cartKey,map);
        //hgetAll --map
        return cartInfoList;
    }
}
