package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @Description 用户业务实现层
 * @auther CQ
 * @create 2019-12-25 下午 6:45
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> queryAllUser() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getAddressByUserId(String id) {
        Example example = new Example(UserAddress.class);
        example.createCriteria().andEqualTo("userId",id);
        return userAddressMapper.selectByExample(example);
    }

}
