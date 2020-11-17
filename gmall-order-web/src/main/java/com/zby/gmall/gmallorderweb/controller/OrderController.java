package com.zby.gmall.gmallorderweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zby.gmall.bean.CartInfo;
import com.zby.gmall.bean.OrderDetail;
import com.zby.gmall.bean.OrderInfo;
import com.zby.gmall.bean.UserAddress;
import com.zby.gmall.config.LoginRequire;
import com.zby.gmall.service.CartService;
import com.zby.gmall.service.OrderService;
import com.zby.gmall.service.UserInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        //加载收货人地址信息
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = userInfoService.getUserAddressList(userAddress);
        request.setAttribute("userAddressList",userAddressList);
        //展示送货清单 orderDetail
        //先通过userId查询那些商品是被选中的 被选中的购物车
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        //用什么集合来存储OrderDetail
        List<OrderDetail> orderDetailList = new ArrayList<>();
        //查询出来之后，给OrderDetail
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        //计算总金额并保存
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        //保存OrderDetail集合给页面渲染！
        request.setAttribute("orderDetailList",orderDetailList);

        //生成流水号并保存到作用域
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);
        return "trade";
    }

    //下订单
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        //获取userId
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);

        //保存之前判断用户是否重复提交

        //获取页面的tradeNo
        String tradeNo = request.getParameter("tradeNo");
        //调用比较方法
        boolean result = orderService.checkTradeNo(tradeNo, userId);
        if(!result){
            //比较失败
            request.setAttribute("errMsg","不能重复提交订单！");
            return "tradeFail";
        }


        //验证库存：http://www.gware.com/hasStock
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null&&orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                boolean res = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
                if(!res){
                   //验证库存失败！
                   request.setAttribute("errMsg",orderDetail.getSkuName()+"库存不足！");
                   return "tradeFail";
                }
                //验证价格：skuInfo.getPrice().equals(orderDetail.getOrderPrice()) true 价格没有变化 false 跟新购物车商品价格
             }
        }

        //多线程出现共享资源抢占的时候才会加锁！
        //是否是会员，是否有优惠券，满减。。。。。 1判断是否有会员 2是否有优惠券 3验证库存 ----异步编排
        
        //调用服务层 保存数据
        String orderId = orderService.saveOrder(orderInfo);

        //删除流水号
        orderService.deleteTradeNo(userId);

        //重定向到支付模块
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    // http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 调用service 层方法
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId,wareSkuMap);

        // 声明一个集合来存储map
        ArrayList<Map> maps = new ArrayList<>();
        if (subOrderInfoList!=null && subOrderInfoList.size()>0){
            // 循环遍历
            for (OrderInfo orderInfo : subOrderInfoList) {
                // 将orderInfo 变为map【json 字符串】
                Map map = orderService.initWareOrder(orderInfo);
                maps.add(map);
            }
        }
        return JSON.toJSONString(maps);
    }


}
