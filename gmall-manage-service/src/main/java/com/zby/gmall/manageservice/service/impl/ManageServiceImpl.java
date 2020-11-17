package com.zby.gmall.manageservice.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zby.gmall.bean.*;
import com.zby.gmall.config.RedisUtil;
import com.zby.gmall.manageservice.constant.ManageConst;
import com.zby.gmall.manageservice.mapper.*;
import com.zby.gmall.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper catalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper catalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper catalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper attrInfoMapper;

    @Autowired
    private BaseAttrValueMapper attrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper saleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired//从容器中获取对象！
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return catalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 catalog2 = new BaseCatalog2();
        catalog2.setCatalog1Id(catalog1Id);
        return catalog2Mapper.select(catalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 catalog3 = new BaseCatalog3();
        catalog3.setCatalog2Id(catalog2Id);
        return catalog3Mapper.select(catalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
//        BaseAttrInfo attrInfo = new BaseAttrInfo();
//        attrInfo.setCatalog3Id(catalog3Id);
//        return attrInfoMapper.select(attrInfo);

        //必须使用xxxMapper.xml形式来解决
        //select * from base_attr_info bai inner join base_attr_value bav
        //on bai.id = bav.attr_id where bai.catalog3_id = 61
        return attrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);

    }

    //添加商品平台属性    修改商品平台属性
    @Override
    @Transactional
    public boolean saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        try {

            //baseAttrInfo
            //有id做修改
            if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {
                attrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
            } else {
                attrInfoMapper.insertSelective(baseAttrInfo);
            }

            //baseAttrValue
            //update baseAttrValue set xx=xxx where attr_id = ? 不用这种方式修改
            //先将baseAttrValue 中的数据根据attr_id删除，然后再插入

            //delete from baseAttrValue where attrId = ?
            BaseAttrValue attrValue = new BaseAttrValue();
            attrValue.setAttrId(baseAttrInfo.getId());
            attrValueMapper.delete(attrValue);
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            if (attrValueList != null && attrValueList.size() > 0) {
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    baseAttrValue.setAttrId(baseAttrInfo.getId());
                    attrValueMapper.insert(baseAttrValue);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //根据平台属性id查询属性值集合
    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        //select * from baseAttrValue where attrId = ?
        BaseAttrValue attrValue = new BaseAttrValue();
        attrValue.setAttrId(attrId);
        return attrValueMapper.select(attrValue);
    }

    //根据attrId获取BaseAttrInfo
    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //select * from baseAttrInfo where id = ?
        BaseAttrInfo baseAttrInfo = attrInfoMapper.selectByPrimaryKey(attrId);
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }


    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    //查询销售属性列表
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return saleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存商品表
        spuInfoMapper.insertSelective(spuInfo);

        //保存销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() > 0) {
            //循环遍历
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //设置spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        //设置spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }

        //spuImage:商品图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

    }


    @Override
    public List<SpuImage> getSpuImgList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    /**
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
//        skuInfo：库存单元表
        skuInfoMapper.insertSelective(skuInfo);
        //skuImage：sku图片
        //一个sku对应一组图片
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() > 0) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

//        skuSaleAttrValue : 销售属性值表 {能够确定sku 具体有那些销售属性值}
//      集合长度size() 数组长度：length 字符串长度 length() 文件长度 length()
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }

//        skuAttrValue：平台属性值关联表 {能够通过平台属性值对sku进行筛选}
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        return getSkuInfoRedisson(skuId);
    }

    /**
     * 使用Redisson框架组分布式锁
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedisson(String skuId) {
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        RLock lock = null;
        try {
            jedis = redisUtil.getJedis();
            //定义key获取数据   sku:Id:info
            String userKey = ManageConst.SKUKEY_PREFIX+ skuId +ManageConst.SKUKEY_SUFFIX;
            if(jedis.exists(userKey)){
                //获取缓存中的数据
                String userJson = jedis.get(userKey);
                if(!StringUtils.isEmpty(userJson)){
                    skuInfo = JSON.parseObject(userJson,SkuInfo.class);
                    return skuInfo;
                }
            }else{
                //创建config
                Config config = new Config();
                config.useSingleServer().setAddress("redis://192.168.83.220:6379");
                RedissonClient redisson = Redisson.create(config);
                lock = redisson.getLock("my-lock");
                lock.lock();

                //从数据库查询数据
                skuInfo = getSkuInfoDB(skuId);
                //将数据放入缓存
                jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                jedis.close();
            }
            if(lock!=null){
                lock.unlock();
            }
        }
        //如果redis宕机，走数据库
        return getSkuInfoDB(skuId);
    }

    /**
     * 使用redis-set做分布式锁
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoRedist(String skuId) {
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        try {
            /*
             *1、获取jedis
             *2、判断缓存中是否有数据
             *3、如果有，从缓存中获取
             *4、没有，走db要加锁，放入redis
             */
            jedis = redisUtil.getJedis();
            //定义key获取数据   sku:Id:info
            String userKey = ManageConst.SKUKEY_PREFIX+ skuId +ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(userKey);
            if(skuJson==null){//redis缓存中没有数据
                //set k1 v1 px 10000 nx
                //定义锁的Key sku:skuId:lock
                String skuLockKey = ManageConst.SKUKEY_PREFIX+ skuId +ManageConst.SKULOCK_SUFFIX;
                //执行命令
                String lockKey = jedis.set(skuLockKey,"zby","NX","PX",ManageConst.SKULOCK_EXPIRE_PX);
                if("OK".equals(lockKey)){//获取到分布式锁！在sku:Id:info在缓存中没有数据的时候，并且sku:skuId:lock不存在
                    //从数据库中取得数据，放入redis中
                    skuInfo = getSkuInfoDB(skuId);
                    //将对象转换成字符串
                    String skuRedisStr = JSON.toJSONString(skuInfo);
                    jedis.setex(userKey,ManageConst.SKUKEY_TIMEOUT,skuRedisStr);
                    //删除掉锁
                    jedis.del(skuLockKey);
                    return skuInfo;
                }else{//在sku:Id:info在缓存中没有数据的时候，并且sku:skuId:lock存在
                    //等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else{//sku:Id:info redis中有缓存
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        //如果redis宕机，走数据库
        return getSkuInfoDB(skuId);
    }

    //ctrl+alt+m
    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        List<SkuImage> skuImageList = getSkuImageBySkuId(skuId);
        skuInfo.setSkuImageList(skuImageList);
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);
        return skuInfo;
    }


    @Override
    public List<SkuImage> getSkuImageBySkuId(String skuId) {
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        return skuImageMapper.select(skuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    //根据spuId查询出SkuSaleAttrValue集合
    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }


    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        /*
        第一种：
        mybatis
        <select id="selectPostIn" resultType="domain.blog.Post">
            SELECT * FROM base_attr_info bai INNER JOIN base_attr_value bav
            ON bai.id = bav.attr_id WHERE bav.id in
                <foreach item="valueId" index="index" collection="list"
                    open="(" separator="," close=")">
                    #{valueId}
                </foreach>
        </select>
         */

        /*
        第二种：13，14，80，81，82，83看作一个字符串直接传入sql语句！
         */
        String valueIds = org.apache.commons.lang3.StringUtils.join(attrValueIdList.toArray(), ",");
        System.out.println("valueIds:"+valueIds);
        return attrInfoMapper.selectAttrInfoListByIds(valueIds);
    }

}
