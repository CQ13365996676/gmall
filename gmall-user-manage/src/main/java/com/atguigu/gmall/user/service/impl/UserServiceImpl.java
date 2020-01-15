package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;
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

    @Autowired
    private RedisUtil redisUtil;

    //redis中的用户Key前缀
    public String userKey_prefix="user:";

    //redis中的用户Key后缀
    public String userinfoKey_suffix=":info";

    //redis中的用户K-V的存活时间
    public int userKey_timeOut=60*60*24;

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

    /**
     * 登录请求,判断数据库中是否有该用户
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        //前端传来的密码是明文，所以需要加密
        String passwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(passwd);
        UserInfo user = userInfoMapper.selectOne(userInfo);
        if(user!=null){
            //还需要将数据保存到redis缓存中
            String userKey = userKey_prefix+user.getId()+userinfoKey_suffix;
            Jedis jedis = redisUtil.getJedis();
            jedis.setex(userKey, userKey_timeOut, JSON.toJSONString(user));
            return user;
        }else {
            return null;
        }
    }

    /**
     * 根据用户ID查询redis中的用户信息
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userKey = userKey_prefix+userId+userinfoKey_suffix;
        String userJson = jedis.get(userKey);
        //判断取出的用户信息是否为空
        if(!StringUtils.isEmpty(userJson)){
            //不为空则将其转换成对象
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }else {
            return null;
        }
    }

}
