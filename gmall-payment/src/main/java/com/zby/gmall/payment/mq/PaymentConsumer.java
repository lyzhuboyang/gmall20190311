package com.zby.gmall.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.PaymentInfo;
import com.zby.gmall.service.PaymentService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        //获取消息队列中的数据
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");
        //根据outTradeNo查询paymentInfo
        //select * from paymentInfo where out_trade_no = ?
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

        //检查是否支付的方法
        boolean result = paymentService.checkPayment(paymentInfoQuery);
        System.out.println("检查结果："+result);
        if(!result&&checkCount>0){
            System.out.println("检查次数："+checkCount);
            //继续检查 在发送消息
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }

    }

}
