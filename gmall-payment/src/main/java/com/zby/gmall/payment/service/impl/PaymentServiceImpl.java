package com.zby.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.github.wxpay.sdk.WXPayUtil;
import com.zby.gmall.bean.OrderInfo;
import com.zby.gmall.bean.PaymentInfo;
import com.zby.gmall.bean.enums.PaymentStatus;
import com.zby.gmall.commonutil.httpclient.HttpClient;
import com.zby.gmall.config.ActiveMQUtil;
import com.zby.gmall.payment.mapper.PaymentInfoMapper;
import com.zby.gmall.service.OrderService;
import com.zby.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    //服务号
    @Value("${appid}")
    private String appid;

    //商户号Id
    @Value("${partner}")
    private String partner;

    //密匙
    @Value("${partnerkey}")
    private String partnerkey;

    /**
     * 保存交易记录信息
     * @param paymentInfo
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        return paymentInfoMapper.selectOne(paymentInfoQuery);
    }

    /**
     * 根据交易编号修改交易记录
     * @param out_trade_no
     * @param paymentInfoUPD
     */
    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    //退款
    @Override
    public boolean refund(String orderId) {
        //根据OrderId查询OrderInfo
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("refund_amount",orderInfo.getTotalAmount());
        map.put("refund_reason","正常退款");
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;

        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    /**
     * 微信支付接口
     * @param orderId
     * @param totalAmount
     * @return
     */
    @Override
    public Map createNative(String orderId, String totalAmount) {
        //根据API文档制作参数
        Map<String,String> map = new HashMap<>();
        map.put("appid",appid);
        map.put("mch_id",partner);
        map.put("nonce_str", WXPayUtil.generateNonceStr());
        map.put("body","买袜子");
        map.put("out_trade_no",orderId);
        map.put("total_fee",totalAmount);
        map.put("spbill_create_ip","127.0.0.1");
        map.put("notify_url","http://order.gmall.com/trade");
        map.put("trade_type","NATIVE");

        try {
            //将参数以xml发送给支付接口

            //将map转换为xml
            String xmlParam = WXPayUtil.generateSignedXml(map, partnerkey);
            //发送到接口上
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            //设置https
            httpClient.setHttps(true);
            //发送xmlParam
            httpClient.setXmlParam(xmlParam);
            //设置发送方式
            httpClient.post();

            //获取支付结果
            String content = httpClient.getContent();
            //将其转换为map
            Map<String, String> xmlToMap = WXPayUtil.xmlToMap(content);

            //声明一个map来存储需要使用的信息
            Map<String,Object> resultMap = new HashMap<>();
            //获取map中的数据
            resultMap.put("code_url",xmlToMap.get("code_url"));
            resultMap.put("total_fee",totalAmount);
            resultMap.put("out_trade_no",orderId);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }

    }

    /**
     * 发送消息给订单
     * @param paymentInfo
     * @param reslut
     */
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String reslut) {
        //创建连接，并打开连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_queue);
            //创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",reslut);
            //发送消息
            producer.send(activeMQMapMessage);
            //提交
            session.commit();
            //关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkPayment(PaymentInfo paymentInfo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        //用一个map集合来存储数据
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //指的是支付宝中有该笔交易 相当于异步回调
        if(response.isSuccess()){
            if("TRADE_SUCCESS".equals(response.getTradeStatus())||"TRADE_FINISHED".equals(response.getTradeStatus())){
                System.out.println("支付成功");
                //更新交易记录的状态
                //改支付状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfo.getOutTradeNo(),paymentInfoUpd);
                //paymentInfo 必须有orderId才能正常发送消息队列给订单
                sendPaymentResult(paymentInfo,"success");
            }
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    /**
     * 发送延迟队列
     * 发送消息查询是否支付成功
     * @param outTradeNo
     * @param delaySec
     * @param checkCount
     */
    //生成二维码之后调用该方法
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        //创建工厂连接，并打开连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            //创建发送消息的对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo",outTradeNo);
            activeMQMapMessage.setInt("delaySec",delaySec);
            activeMQMapMessage.setInt("checkCount",checkCount);

            //设置延迟队列的时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);

            //发送消息
            producer.send(activeMQMapMessage);
            //必须提交
            session.commit();
            //关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /**
     * 根据OrderId关闭交易记录信息
     * @param ordreId
     */
    @Override
    public void closePayment(String ordreId) {
        //update paymentInfo set PaymentStatus=PaymentStatus.CLOSED where orderId = ?
        //paymentInfo 更新的内容
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        //example设置条件
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",ordreId);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

}
