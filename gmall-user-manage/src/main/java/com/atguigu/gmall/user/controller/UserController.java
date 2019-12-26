package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-25 下午 6:53
 */
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("/queryAllUser")
    public List<UserInfo> queryAllUser(){
        return userService.queryAllUser();
    }

}
