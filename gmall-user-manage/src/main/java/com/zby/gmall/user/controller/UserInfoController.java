package com.zby.gmall.user.controller;

import com.zby.gmall.bean.UserInfo;
import com.zby.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class UserInfoController {
    @Autowired
    UserInfoService userInfoService;

    //如果是@Controller注解，会跳转到list界面
//    @RequestMapping("findAll")
//    public String  findAll(){
//         userInfoService.findAll();
//        return "list";
//    }

    //方法的返回值，方法的传入参数
    //localhost:8080/findAll
    @RequestMapping("findAll")
    public List<UserInfo> findAll() {
        return userInfoService.findAll();
    }

    //localhost:8080/findUserByUserInfo?name=admin
    @RequestMapping("findUserInfoListByUserInfo")
    public List<UserInfo> getUserInfoListByUserInfo(UserInfo userInfo, HttpServletRequest request) {
        //String name = request.getParameter("name");
        //select * from userInfo where name = ?
        return userInfoService.getUserInfoListByUserInfo(userInfo);
    }

    //localhost:8080/findUserInfoListByNickName?nickName=a
    @RequestMapping("findUserInfoListByNickName")
    public List<UserInfo> getUserInfoListByNickName(UserInfo userInfo) {
        return userInfoService.getUserInfoListByNickName(userInfo);
    }

    //localhost:8080/addUser?name=eh&loginName=yx
    @RequestMapping("addUser")
    public void addUser(UserInfo userInfo) {
        userInfoService.addUser(userInfo);
    }

    //localhost:8080/updateUserById?id=1&nickName=9999
    @RequestMapping("updateUserById")
    public void udpUser(UserInfo userInfo) {
        userInfoService.updateUserById(userInfo);
    }


    //localhost:8080/updateUserByUser?nickName=9999&name=66666
    @RequestMapping("updateUserByUser")
    public void updateUserByUser(UserInfo userInfo) {
        userInfoService.updateUserByUser(userInfo);
    }

    //localhost:8080/deleteUser?name=66666
    @RequestMapping("deleteUser")
    public void deleteUser(UserInfo userInfo) {
        userInfoService.delUser(userInfo);
    }
}
