package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-26 上午 10:09
 */
@RestController
public class OrderController {

    @Reference
    private UserService userService;

    /**
     * 根据用户ID获取用户的所有地址
     * @param id
     * @return
     */
    @GetMapping("/getAddressByUserId")
    public List<UserAddress> getAddressByUserId(String id){
        return userService.getAddressByUserId(id);
    }

}
