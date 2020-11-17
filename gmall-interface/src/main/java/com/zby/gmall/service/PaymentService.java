package com.zby.gmall.service;

import com.zby.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    //保存交易记录
    void savePaymentInfo(PaymentInfo paymentInfo);

    //查询交易记录
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    //更改交易记录
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    //退款接口
    boolean refund(String orderId);

    /**
     * 微信支付接口
     * @param orderId
     * @param totalAmount
     * @return
     */
    Map createNative(String orderId, String totalAmount);

    /**
     * 发送消息给订单
     * @param paymentInfo
     * @param reslut
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String reslut);

    /**
     * 根据 out_trade_no查询
     * @param paymentInfo
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfo);

    /**
     * 发送延迟队列
     * 发送消息查询是否支付成功
     * @param outTradeNo
     * @param delaySec
     * @param checkCount
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 关闭交易记录
     * @param id
     */
    void closePayment(String id);
}
