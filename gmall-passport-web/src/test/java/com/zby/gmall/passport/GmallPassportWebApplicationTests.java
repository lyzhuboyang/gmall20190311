package com.zby.gmall.passport;

import com.zby.gmall.passport.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

    @Test
    public void contextLoads() {

    }

    @Test
    public void testJwT(){
        String key = "atguigu";
        String ip="192.168.83.220";
        Map map = new HashMap();
        map.put("userId","1001");
        map.put("nickName","marry");
        String token = JwtUtil.encode(key, map, ip);
        System.out.println("token："+token);

        //解密出来的是用户信息
        Map<String, Object> objectMap = JwtUtil.decode(token, key, "192.168.83.220");
        System.out.println(objectMap.get("userId"));
        System.out.println(objectMap.get("nickName"));

    }

}
