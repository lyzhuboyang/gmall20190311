package com.zby.gmall.commonutil.exception;

import com.zby.gmall.commonutil.data.R;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R error(Exception e) {
        e.printStackTrace();
        return R.error().message("出错了");
    }

//    @ExceptionHandler(Exception.class)
//    @ResponseBody
//    public R error(ClassCastException e) {
//        e.printStackTrace();
//        return R.error().message("类型转换出错了");
//    }

}
