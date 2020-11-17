package com.zby.gmall.service;

import com.zby.gmall.bean.UserAddress;
import com.zby.gmall.bean.UserInfo;

import java.util.List;

//业务逻辑层
public interface UserInfoService {

    /**
     * 查询所有用户数据
     *
     * @return
     */
    List<UserInfo> findAll();

    /**
     * 根据userInfo查询
     *
     * @param userInfo
     * @return
     */
    List<UserInfo> getUserInfoListByUserInfo(UserInfo userInfo);

    /**
     * 模糊查询
     *
     * @param userInfo
     * @return
     */
    List<UserInfo> getUserInfoListByNickName(UserInfo userInfo);

    /**
     * 添加接口的返回值：void  int  boolean
     */
    void addUser(UserInfo userInfo);

    /**
     * 根据id修改
     *
     * @param userInfo
     */
    void updateUserById(UserInfo userInfo);

    /**
     * 根据属性修改
     *
     * @param userInfo
     */
    void updateUserByUser(UserInfo userInfo);

    /**
     * 删除
     *
     * @param userInfo
     */
    void delUser(UserInfo userInfo);

    /**
     * 根据用户id查询用户地址
     *
     * @param userAddress
     * @return
     */
    List<UserAddress> getUserAddressList(UserAddress userAddress);

    /**
     * 用户登录方法
      * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);


    /**
     * 认证方法
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
