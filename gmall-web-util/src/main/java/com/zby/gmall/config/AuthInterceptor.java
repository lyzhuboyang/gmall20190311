package com.zby.gmall.config;

import com.alibaba.fastjson.JSON;
import com.zby.gmall.commonutil.httpclient.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {


    //表示进入控制器之前
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //http://item.gmall.com/37.html?newToken=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IuW8oOS4iSIsInVzZXJJZCI6IjMifQ.Sm2A8haRaefg6TiWiBb1pAs8ouZ7MIfZ_J2FAkC2kLY

        //表示登录成功之后，获取newTok
        String token = request.getParameter("newToken");
        //当token不为空的时候，将token放入cookieh中
        if(token!=null){
            //token中没有中文，isEncode设置false
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }

        //http://item.gmall.com/37.html
        if(token==null){
            //cookie有可能存在taoken
            token = CookieUtil.getCookieValue(request,"token",false);
        }

        //从token中获取用户名称
        if(token!=null){
            //获取nickName
            Map map = getUserMapByToken(token);
            String nickName = (String)map.get("nickName");
            //保存nickName
            request.setAttribute("nickName",nickName);
        }

        //判断当前控制器上是否有注解LoginRequire
        HandlerMethod handlerMethod = (HandlerMethod)handler;
        //看方法上是否有注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if(methodAnnotation!=null){//有注解，没有登录必须登录
            //获取到的注解
            //认证：用户是否登录的认证 调用passPortController中verify控制器
            //http://passport.atguigu.com/verify?token=XXX&salt=xxx
            String salt = request.getHeader("X-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if("success".equals(result)){
                //保存一下userId
                Map map = getUserMapByToken(token);
                String userId = (String)map.get("userId");
                //保存userId
                request.setAttribute("userId",userId);
                return true;//拦截器不拦截，放行
            }else{//没有认证成功
                if(methodAnnotation.autoRedirect()){
                    //必须登录！
                    //获取浏览器url
                    String requestURL = request.getRequestURL().toString();
                    //对url进行转码
                    String encodeURL = URLEncoder.encode(requestURL,"UTF-8");
                    //重定向到登录页面 http://passport.atguigu.com/index?originUrl=xxxxx
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;//多个拦截器时，先进后出，A B C A全为true，才会走B
                }
            }
        }
        return true;
    }

    //表示进入控制器之后，返回视图之前
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    //返回视图之后
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }

    /**
     * 获取map集合
     * @return
     */
    private Map getUserMapByToken(String token) {
        // eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IuW8oOS4iSIsInVzZXJJZCI6IjMifQ.Sm2A8haRaefg6TiWiBb1pAs8ouZ7MIfZ_J2FAkC2kLY
        // map属于第二部分 可以使用JWTUtil 工具类 ，base64编码
        // 获取token 中第二部分 数据
        String tokenUserInfo = StringUtils.substringBetween(token,".");
        //创建base64对象
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        //返回字节数组
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);

        //将字节数组转化为String
        String tokenJson = null;
        try {
            tokenJson = new String(decode,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return JSON.parseObject(tokenJson,Map.class);
    }

}
