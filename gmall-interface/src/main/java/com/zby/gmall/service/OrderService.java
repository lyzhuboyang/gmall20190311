package com.zby.gmall.service;

import com.zby.gmall.bean.OrderInfo;
import com.zby.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {

     void updateOrderStatus(String orderId, ProcessStatus paid);
     
    /**
     * 保存订单
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 存储流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);


    /**
     * 比较流水号
     * @param tradeNo
     * @param userId
     * @return
     */
    boolean checkTradeNo(String tradeNo,String userId);

    /**
     * 删除流水号
     * @param userId
     * @return
     */
    void deleteTradeNo(String userId);

    /**
     * 验证库存
      * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId,Integer skuNum);

    /**
     * 根据订单Id查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     *发送消息告诉库存系统：减库存！
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 将orderInfo 转换为map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 查询过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);
}
