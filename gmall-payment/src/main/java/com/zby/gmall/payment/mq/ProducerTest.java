package com.zby.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {
    //主函数
    public static void main(String[] args) {
        /**
         * 1创建消息队列工厂
         * 2获取连接
         * 3打开连接
         * 4创建session
         * 5创建队列
         * 6创建提供者
         * 7创建消息对象
         * 8发送消息
         * 9关闭
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
            //6创建提供者
            MessageProducer producer = session.createProducer(atguigu);
            //7创建消息对象
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            textMessage.setText("123,小明");
            //8发送消息
            producer.send(textMessage);

            //关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
