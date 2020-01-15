package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-07 上午 10:06
 */
@Controller
public class PassportController {

    @Value("${token.key}")
    private String tokenKey;

    @Reference
    private UserService userService;

    /**
     * 访问登录页面
     * @return
     */
    @RequestMapping("/index")
    public String index(HttpServletRequest request){
        //访问该登录页面需要获取从哪个页面跳转过来的地址
        String originUrl = request.getParameter("originUrl");
        //将地址存放到域中
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    /**
     * 登录请求
     * @param userInfo
     * @return
     */
    @ResponseBody
    @RequestMapping("/login")
    public String login(UserInfo userInfo,HttpServletRequest request){
        //调用用户服务
        UserInfo user = userService.login(userInfo);
        //判断该用户在数据库中是否存在，生成token返回给前端
        if(user != null){
            //token中需要存放的数据
            Map<String, Object> map = new HashMap<>();
            map.put("userId",user.getId());
            map.put("nickName",user.getNickName());
            //获取salt（为该应用的IP）
            String salt = request.getHeader("X-forwarded-for");
            //使用工具类生成token并返回
            String token = JwtUtil.encode(tokenKey,map,salt);
            return token;
        }else {
            return "fail";
        }
    }

    /**
     * 用户认证，利用token判断redis中是否有用户信息
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/verify")
    public String verify(HttpServletRequest request){
        //1.获取salt
        String salt = request.getParameter("salt");
        //2.将token进行解密
        String token = request.getParameter("token");
        Map<String, Object> map = JwtUtil.decode(token, tokenKey, salt);
        //3.判断map是否有值
        if(map!=null&&map.size()>0){
            //有值则调用用户服务查询redis中的用户信息
            String userId = (String) map.get("userId");
            UserInfo userInfo = userService.verify(userId);
            //判断获取的用户是否为空
            if(userInfo!=null){
                return "success";
            }
        }
        return "fail";
    }

}
