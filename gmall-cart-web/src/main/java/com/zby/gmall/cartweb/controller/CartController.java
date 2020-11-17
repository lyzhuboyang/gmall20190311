package com.zby.gmall.cartweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.CartInfo;
import com.zby.gmall.bean.SkuInfo;
import com.zby.gmall.config.LoginRequire;
import com.zby.gmall.service.CartService;
import com.zby.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    //记录未登录的userId 给UUID
    private String userKey;


    //控制器是谁？ item.html!在实际开发过程中，从实际业务中取中找
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //应该将对应的商品信息做一个保存
        //调用服务层将商品添加到redis,mysql
        //获取userId
        String userId = (String) request.getAttribute("userId");
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        if(userId!=null){
            //登录状态添加购物车！
            cartService.addToCart(skuId,userId, Integer.parseInt(skuNum));
        }else{
            //未登录添加购物车！放入cookie中
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));

//            userKey = getUUID(request);
//            //将其放入cookie
//            Cookie cookie = new Cookie("userKey",userKey);
//            //将cookie写给客户端
//            response.addCookie(cookie);
//            //放入redis中
//            cartService.addToCartRedis(skuId,userKey,Integer.parseInt(skuNum));

        }
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    //获取Cookie中的UUID
    private String getUUID(HttpServletRequest request) {
        //获取Cookie中的UUID
        Cookie[] cookies = request.getCookies();
        boolean isMatch = false;
        for (Cookie ck : cookies) {
            if(ck.getName().equals("user-key")) {
                userKey = ck.getValue();
                isMatch = true;
            }
        }
        if(!isMatch){
            //未登录放入redis
            userKey = UUID.randomUUID().toString().replace("_","");
        }
        return userKey;
    }


    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        List<CartInfo> cartInfoList = new ArrayList<>();
        String userId = (String)request.getAttribute("userId");
        if(userId!=null){//登录状态
            //先看未登录购物车中是否有数据
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if(cartListCK!=null&&cartListCK.size()>0){//未登录的购物车有数据
                //合并购物车  未登录中有数据+登录中也有数据
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
                // 删除未登录数据
                cartCookieHandler.deleteCartCookie(request,response);
            }else{//未登录中没有数据
                //已登录
                //redis-mysql
                cartInfoList  = cartService.getCartList(userId);
            }
        }else{//未登录状态
            //cookie
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        //保存购物车集合
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }


    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        //从页面传递过来的数据
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");
        if(userId!=null){
            //登录时选中 redis
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            //未登录时选中
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //获取userId
        String userId = (String) request.getAttribute("userId");
        //合并购物车中勾选的商品 cookie---redis合并
        //获取未登录的数据
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        if(cartListCK!=null&&cartListCK.size()>0){
            //合并勾选的商品
            cartService.mergeToCartList(cartListCK,userId);
            //删除未登录的数据
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }


}
