package com.zby.gmall.service;

import com.zby.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    /**
     * 添加
     *返回值，参数列表
     *
     * @param skuId 商品Id
     * @param userId 用户Id
     * @param skuNum 商品数量
     */
   void addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 根据userId查询购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 放入redis
     * @param skuId
     * @param userKey
     * @param parseInt
     */
    void addToCartRedis(String skuId, String userKey, int parseInt);

    /**
     * 选中商品
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据用户Id查询选中商品
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}
