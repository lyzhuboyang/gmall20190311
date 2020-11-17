package com.zby.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zby.gmall.bean.UserInfo;
import com.zby.gmall.passport.config.JwtUtil;
import com.zby.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@CrossOrigin
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    public String key;

    //http://localhost:8087/index?orignUrl=xxxxxxx
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        /**
         * 用户点击登录的时候，必须从页面的某个链接访问登录模块：则登录url后面必须有你点击的那个链接
         */
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    //使用对象传值
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        //调用服务层获取方法
        UserInfo user = userInfoService.login(userInfo);
        if(user!=null){
            //创建key map salt
            //服务器IP地址在服务器中的设置 X-forward-for对应的值，用域名才可以拿到盐
            String salt = request.getHeader("X-forwarded-for");
            Map<String,Object> map  = new HashMap<>();
            map.put("userId", user.getId());
            map.put("nickName", user.getNickName());
            String token = JwtUtil.encode(key, map, salt);
            return token;
        }else{
            return "fail";
        }
    }


    //http://passport.atguigu.com/verify?token=xxxx&salt=xxx
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        //解密Token
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        if(map!=null&&map.size()>0){
            //获取用户Id
            String userId = (String) map.get("userId");
            //调用认证方法
            UserInfo userInfo = userInfoService.verify(userId);
            if(userInfo!=null){
                return "success";
            }
        }
        return "fail";
    }

}
