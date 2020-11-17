package com.zby.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ConsumerTest {
    public static void main(String[] args) {
        /**
         * 1创建消息队列工厂
         * 2获取连接
         * 3打开连接
         * 4创建session
         * 5创建队列
         * 6创建消费者
         * 7接收消息
         */

        //1创建消息队列工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.83.220:61616");
        try {
            //2获取连接
            Connection connection = activeMQConnectionFactory.createConnection();
            //3打开连接
            connection.start();
            //4创建session
            //第一个参数表示是否开启事务，第二个参数表示开启事务所对应的处理方式
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //5创建队列
            Queue atguigu = session.createQueue("test-gmall");
            MessageConsumer consumer = session.createConsumer(atguigu);

            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    //判断消息的类型
                    if(message instanceof TextMessage){
                      String text = null;
                        try {
                            text = ((TextMessage)message).getText();
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                        System.out.println(text);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

}
