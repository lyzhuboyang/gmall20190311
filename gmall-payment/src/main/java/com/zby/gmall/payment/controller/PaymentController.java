package com.zby.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.zby.gmall.bean.OrderInfo;
import com.zby.gmall.bean.PaymentInfo;
import com.zby.gmall.bean.enums.PaymentStatus;
import com.zby.gmall.payment.config.AlipayConfig;
import com.zby.gmall.payment.config.IdWorker;
import com.zby.gmall.service.OrderService;
import com.zby.gmall.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static org.apache.catalina.manager.Constants.CHARSET;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;


    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        //存储总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        //存储订单
        request.setAttribute("orderId",orderId);
        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody//两个作用：返回Json 把信息直接拽到页面
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //数据保存！将交易信息保存到paymentInfo中
        PaymentInfo paymentInfo = new PaymentInfo();
        //paymentInfo 数据来源应该在orderInfo
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("买袜子--绿色的");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());
        paymentService.savePaymentInfo(paymentInfo);

        //根据OrderId查询数据，并

        //显示二维码
        //AlipayClient 注入到Spring容器中！
        //AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //同步回调路径
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调路径
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        //设置参数
        //设置一个map
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        alipayRequest.setBizContent(JSON.toJSONString(map));

        String form= "" ;
        try  {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType( "text/html;charset="  + CHARSET);
        //每个15秒，发送一个队列，检查3次
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    //同步回调 通知用户
    @RequestMapping("/alipay/callback/return")
    public String callbackReturn(){
        //重定向到
        return "redirect:"+ AlipayConfig.return_order_url;
    }


    // 异步回调：通知商家是否支付成功！
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request)  {
        //将异步通知中收到的所有参数都存放到map中

        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset,AlipayConfig.sign_type);//调用SDK验证签名
            //获取交易状态
            String trade_status = paramMap.get("trade_status");
            //商户订单号  交易编号
            String out_trade_no = paramMap.get("out_trade_no");

            if(signVerified) {
                // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

                if("TRADE_SUCCESS".equals(trade_status)||"TRADE_FINISHED".equals(trade_status)){
                    //进一步判断：记录交易日志表的交易状态
                    //select * from paymentInfo where out_trade_no = ?
                    //调用服务层
                    PaymentInfo paymentInfoQuery = new PaymentInfo();
                    paymentInfoQuery.setOutTradeNo(out_trade_no);
                    PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                    if(paymentInfo.getPaymentStatus()==PaymentStatus.PAID||paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED){
                        return "failure";
                    }
                    //支付成功应该修改交易状态
                    //update paymentInfo set getPaymentStatus = PaymentStatus.PAID where out_trade_no = out_trade_no
                    PaymentInfo paymentInfoUPD = new PaymentInfo();
                    paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                    paymentInfoUPD.setCreateTime(new Date());
                    //更新交易记录
                    paymentService.updatePaymentInfo(out_trade_no,paymentInfoUPD);
                    //支付成功！ 订单状态变成支付！ 发送消息队列！
                    paymentService.sendPaymentResult(paymentInfoQuery,"success");
                    return "success";// 给支付宝服务器返回我们处理的结果。如果不返回结果。我们支付宝服务器会在24小时内发送7次通知。
                }
            }else {
                // TODO 验签失败则记录异常日志，并在response中返回failure.
                return "failure";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "failure";
    }

    // 退款 payment.gmall.com/refund?orderId=100
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        //调用服务层接口
        boolean result = paymentService.refund(orderId);
        if(result){
            return "退款成功";
        }else{
            return "退款失败";
        }

    }

    /**
     class Student{
        private int id; id = 1;
        private String name; name=admin
     }
     Map map = new HashMap();
     map.put(id,1);
     map.put(name,admin);
     */
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(HttpServletRequest request){

        /*
        1.  获取交易记录状态：PAID
        2.  PAID :
         */
        /*
        IF(!PAID){
            // 微信支付 缓存中 orderId ，status！
        }else{
            //
        }

         */

        String orderId = request.getParameter("orderId");
        //调用服务层
        // 第一个参数是订单Id，第二个参数是多少钱 单位分
        IdWorker idWorker = new IdWorker();
        long id = idWorker.nextId();
        Map map = paymentService.createNative(id+"","1");
        System.out.println(map.get("code_url"));
        return map;
    }

    @RequestMapping("bank")
    @ResponseBody
    public Map bank(){
        // 第三方接口：
        // 密钥，参数：总金额，商户Id，订单Id，。。。、

        return null;
    }

    //http://payment.gmall.com/sendPaymentResult?orderId=121&result=success
    //手动调用消息队列先进行测试
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentReslut(PaymentInfo paymentInfo,String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "ok";
    }

    //查询当前交易是否已经付款
    //payment.gmall.com/queryPaymentReslut?orderId=xxx 
    @RequestMapping("queryPaymentReslut")
    @ResponseBody
    public String queryPaymentReslut(PaymentInfo paymentInfo){

        //需要根据orderId查询出整个paymentInfo对象
        //select * from paymentInfo wehe orderId=?
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        boolean flag = false;
        if(paymentInfoQuery!=null){
            //需要outTradeNo,orderId
            flag = paymentService.checkPayment(paymentInfoQuery);
        }
        //返回结果
        return ""+ flag;
    }

}
