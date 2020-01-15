package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

/**
 * @Description 用户认证的拦截器
 * @auther CQ
 * @create 2020-01-07 下午 6:26
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     *在用户访问控制器之前就进行拦截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取参数newToken（如果有值并不为空则说明刚登录），将newToken的值保存到cookie中
        String token = request.getParameter("newToken");
        if(token!=null&&token.length()>0){
            //第一种：使用工具类
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
            //第二种：手动添加Cokkie
//            Cookie cookie = new Cookie("token", token);
//            cookie.setMaxAge(WebConst.COOKIE_MAXAGE);
//            response.addCookie(cookie);
        }
        //2.如果参数newToken为空或者不存在，则需要判断cookie中是否有token
        if(token == null){
            //第一种：使用工具类
            token = CookieUtil.getCookieValue(request, "token", false);
            //第二种：手动获取Cokkie
//            Cookie[] cookies = request.getCookies();
//            for (Cookie cookie : cookies) {
//                String name = cookie.getName();
//                if ("token".equals(name)){
//                    token  = cookie.getValue();
//                }
//            }
        }
        //3.如果cookie中的token不为空，则发送请求进行认证
        if(token!=null){
            //对token进行解密，获取用户信息
            Map<String,Object> map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName",nickName);
        }
        //-------------------------以上是单点登录------------------------------
        //4.根据业务需求判断哪些Controller需要登录
        //4.1获取该控制器上的注解方法
        HandlerMethod method = (HandlerMethod) handler;
        LoginRequire loginRequire = method.getMethodAnnotation(LoginRequire.class);
        //4.2判断是否有这个方法
        if(loginRequire!=null){
            //获取盐值
            String salt = request.getHeader("X-forwarded-for");
            //远程调用认证接口
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token+"&salt="+salt);
            //如果返回值为success则直接放行
            if("success".equals(result)){
                // 用户已经登录状态！
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                // 保存到作用域
                request.setAttribute("userId",userId);
                //放行
                return true;
            }else{
                //如果返回值不为success则需要判断该注解的值是否为true
                if(loginRequire.autoRedirect()){
                    //获取当前的请求URL
                    String requestURL = request.getRequestURL().toString();
                    //将请求URL转码
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    //重定向到登陆页面
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 对token进行解密，获取用户信息
     * @param token
     * @return
     */
    private Map<String, Object> getUserMapByToken(String token) {
        //token由三部分组成，其中中间部分保存我们需要的数据（格式为xxx.xxx.xxx），所以获取中间部分
        String tokenUserInfo = StringUtils.substringBetween(token, ".");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);
        //将bytes转化为String类型，然后转换成map
        String tokenJson = null;
        try {
            tokenJson = new String(bytes,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class);
        return map;
    }

}
