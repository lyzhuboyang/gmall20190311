package com.zby.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zby.gmall.bean.UserAddress;
import com.zby.gmall.bean.UserInfo;
import com.zby.gmall.config.RedisUtil;
import com.zby.gmall.service.UserInfoService;
import com.zby.gmall.user.mapper.UserAddressMapper;
import com.zby.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> getUserInfoListByUserInfo(UserInfo userInfo) {
        return userInfoMapper.select(userInfo);
    }

    @Override
    public List<UserInfo> getUserInfoListByNickName(UserInfo userInfo) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("nickName", "%" + userInfo.getNickName() + "%");
        return userInfoMapper.selectByExample(example);
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userInfoMapper.insertSelective(userInfo);
    }

    //根据主键值修改
    @Override
    public void updateUserById(UserInfo userInfo) {
        userInfoMapper.updateByPrimaryKeySelective(userInfo);
    }

    //根据其他值修改
    @Override
    public void updateUserByUser(UserInfo userInfo) {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andEqualTo("nickName", userInfo.getNickName());
        userInfoMapper.updateByExampleSelective(userInfo, example);
    }


    @Override
    public void delUser(UserInfo userInfo) {
        userInfoMapper.delete(userInfo);
    }


    @Override
    public List<UserAddress> getUserAddressList(UserAddress userAddress) {
        return userAddressMapper.select(userAddress);
    }

    //用户登录
    @Override
    public UserInfo login(UserInfo userInfo) {
        //select * from userInfo where userName = ? and password  =?
        //将密码变成加密之后的
        String passwd = userInfo.getPasswd();
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPasswd);

        //数据库中放的是加密后的密码
        UserInfo info = userInfoMapper.selectOne(userInfo);

        if(info!=null){
            Jedis jedis = null;
            //放入redis
            try {
                jedis = redisUtil.getJedis();
                //定义key 存储用户 sku:skuId:info  |  user:userId:info
                String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;
                //redis使用String类型来存储数据
                jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                jedis.close();
            }
            return info;
        }
        return null;
    }

    /**
     * 调用认证方法
     * @param userId
     * @return
     */
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        UserInfo userInfo = null;
        //定义key
        String userKey = userKey_prefix+userId+userinfoKey_suffix;
        String userJson = jedis.get(userKey);

        if(!StringUtils.isEmpty(userJson)){
            //延长用户过期时间
           jedis.expire(userKey,userKey_timeOut);
           userInfo= JSON.parseObject(userJson,UserInfo.class);
           return userInfo;
        }
        jedis.close();
        return userInfo;
    }


}
