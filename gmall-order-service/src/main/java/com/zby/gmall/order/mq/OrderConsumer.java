package com.zby.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.enums.ProcessStatus;
import com.zby.gmall.service.OrderService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;


//消费消息
@Component
public class OrderConsumer {
    @Reference
    private OrderService orderService;

    //启动消息监听器 new MessageListener()
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentReslut(MapMessage mapMessage) throws JMSException {
        //获取消息队列中的数据
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");
        //判断支付结果
        if("success".equals(result)){
            //更新订单状态 已支付
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            //发送消息告诉库存系统：减库存！
            orderService.sendOrderStatus(orderId);
            orderService.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        }
    }

    //启动消息监听器 new MessageListener()
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerSkuDeduct(MapMessage mapMessage) throws JMSException {
        //获取消息队列中的数据
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");
        //判断减库存结果
        if("DEDUCTED".equals(status)){
            //更新订单状态 待发货
            orderService.updateOrderStatus(orderId, ProcessStatus.WAITING_DELEVER);
        }
    }

}
