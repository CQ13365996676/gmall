package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

/**
 * 用户业务接口层
 */
public interface UserService {

    /**
     * 查询所有的用户信息
     * @return
     */
    List<UserInfo> queryAllUser();

    /**
     * 根据用户ID获取用户的所有地址
     * @param id
     * @return
     */
    List<UserAddress> getAddressByUserId(String id);

    /**
     * 登录请求
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 根据用户ID查询redis中的用户信息
     * @param userId
     * @return
     */
    UserInfo verify(String userId);

}
