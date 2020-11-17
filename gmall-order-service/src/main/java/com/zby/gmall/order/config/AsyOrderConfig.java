package com.zby.gmall.order.config;

import lombok.SneakyThrows;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration//开启异步
public class AsyOrderConfig implements AsyncConfigurer {

    // Executor 执行者
    /**
     * 执行者
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        //定义多线程
        //获取线程池-数据库的连接池
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        //设置线程数
        threadPoolTaskExecutor.setCorePoolSize(10);
        //设置最带连接数
        threadPoolTaskExecutor.setMaxPoolSize(100);
        //设置等待队列，如果10个不够，可以有100个线程等待缓冲池
        threadPoolTaskExecutor.setQueueCapacity(100);
        //初始化操作
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    /**
     * 处理异常
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        //自定义异常
        //throw new MyException("哈哈错误了");
        return null;
    }

}

//继承
class MyException extends Error{
    //class MyException extends Error{
    String msg;
    MyException(String message){
        //获取父类异常信息
        this.msg = message;
    }
    public void getMsg(){
        // 获取父类的异常信息
        super.getMessage();
    }
}

