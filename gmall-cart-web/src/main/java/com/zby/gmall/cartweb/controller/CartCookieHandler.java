package com.zby.gmall.cartweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zby.gmall.bean.CartInfo;
import com.zby.gmall.bean.SkuInfo;
import com.zby.gmall.config.CookieUtil;
import com.zby.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

//操作未登录的数据
@Component
public class CartCookieHandler {

    @Reference
    private ManageService manageService;

    //定义购物车名称
    private String cookieCartName = "CART";
    //设置cookie过期时间
    private int COOKIE_CART_MAXAGE = 7*24*3600;
    /**
     * @param request
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        /**
         *1、判断购物车是否有该商品 通过skuId去cookie中循环比较
         *2、有：数量相加
         *3、没有：直接添加到cookie!
         */
         //获取购物车中的所有数据
        // Cookie cookie = new Cookie(cookieName, cookieValue);
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        //定义一个boolean类型的变量
        boolean ifExist = false;
        List<CartInfo> cartInfoList = new ArrayList<>();
        if(StringUtils.isNotEmpty(cookieValue)){
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            //通过SkuId去cookie中循环比较
            for (CartInfo cartInfo : cartInfoList) {
                if(cartInfo.getSkuId().equals(skuId)) {
                    //数量添加
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist = true;
                }
            }
        }
        //不存在！
        if(!ifExist){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //数据赋值
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfoList.add(cartInfo);
        }

        //写入cookie
        CookieUtil.setCookie(request,response,cookieCartName,JSON.toJSONString(cartInfoList),COOKIE_CART_MAXAGE,true);
    }

    /**
     * 获取购物车数据
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        if(StringUtils.isNotEmpty(cookieValue)){
            cartInfoList = JSON.parseArray(cookieValue,CartInfo.class);
        }
        return cartInfoList;
    }

    /**
     * 删除未登录时，cookie中的购物车中的数据
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    /**
     * 未登录选中
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //先获取购物车中的集合
        List<CartInfo> cartList = getCartList(request);
        if(cartList!=null&&cartList.size()>0){
            for (CartInfo cartInfo : cartList) {
                //判断购物车中是否有相同商品
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isChecked);
                }
            }
        }
        //将其存储购物车
        CookieUtil.setCookie(request,response,cookieCartName, JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);
    }
}
