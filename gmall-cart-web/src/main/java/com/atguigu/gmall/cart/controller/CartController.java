package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-08 下午 4:44
 */
@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    /**
     * 将商品添加到购物车中
     * 添加购物车有两种情况：1.已登录  2.未登录
     * @param request
     * @return
     */
    @RequestMapping("/addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //1.获取需要添加的skuId和skuNum
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        //2.获取域中的userId
        String userId = (String) request.getAttribute("userId");
        //3.如果userId为空则需要从Cookie中获取匿名userId
        if(userId == null){
            userId = CookieUtil.getCookieValue(request, "userKey", false);
            //4.如果匿名userId也为空则需要创建一个匿名userId，并将其保存到Cookie中
            if(userId == null){
                userId = UUID.randomUUID().toString().replaceAll("-", "");
                CookieUtil.setCookie(request,response,"userKey",userId,7*24*3600,false);
            }
        }
        cartService.addToCart(skuId,userId,skuNum);
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    /**
     * 查询购物车列表
     * @return
     */
    @RequestMapping("/cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        //1.获取域中的userId
        String userId = (String) request.getAttribute("userId");
        //2.如果userId不为空则直接通过userId查询，并且合并未登录的数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        if(userId != null){
            String userTempId = CookieUtil.getCookieValue(request, "userKey", false);
            //如果临时用户Id不存在则直接返回已登录的集合即可
            if(StringUtils.isEmpty(userTempId)){
                cartInfoList = cartService.getCartList(userId);
            }else{
                //获取未登录的购物车列表集合
                List<CartInfo> cartInfoNoLoginList = cartService.getCartList(userTempId);
                //合并已登录和未登录的购物车列表集合，并且删除未登录的购物车列表集合
                if(cartInfoNoLoginList!=null&&cartInfoNoLoginList.size()>0){
                    cartInfoList = cartService.mergeToCartList(cartInfoNoLoginList,userId);
                    cartService.deleteCartList(userTempId);
                }else{
                    //集合为空则直接返回已登录的购物车列表
                    cartInfoList = cartService.getCartList(userId);
                }
            }
        }else{
            //如果userId为空则去Cookie中取匿名userId
            userId = CookieUtil.getCookieValue(request, "userKey", false);
            //如果userId为空则直接通过userId查询
            if(userId != null){
                cartInfoList = cartService.getCartList(userId);
            }
        }
        //将数据保存到request域中
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    /**
     * 根据userId和skuId更改列表数据的状态（选中还是未选中）
     * 异步请求需要添加一个@ResponseBody注解
     */
    @ResponseBody
    @RequestMapping("/checkCart")
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request){
        //1.获取传递过来的参数
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        //2.获取域中的userId
        String userId = (String) request.getAttribute("userId");
        //3.判断userId是否为空
        if(StringUtils.isEmpty(userId)){
            //如果userId为空则从Cookie中查询临时Id
            userId = CookieUtil.getCookieValue(request, "userKey", false);
        }
        cartService.checkCart(skuId,isChecked,userId);
    }

    /**
     * 点击去结算时需要根据用户登录还是没登录去查看购物车里的数据
     * @param request
     * @return
     */
    @LoginRequire
    @RequestMapping("/toTrade")
    public String toTrade(HttpServletRequest request){
        //1.获取域中的userId和Cookie中的临时Id
        String userId = (String) request.getAttribute("userId");
        String userTempId = CookieUtil.getCookieValue(request, "userKey", false);
        //2.如果临时用户Id不为空则需要将两者合并
        if(!StringUtils.isEmpty(userTempId)){
            //获取未登录的购物车列表集合
            List<CartInfo> cartInfoNoLoginList = cartService.getCartList(userTempId);
            //合并已登录和未登录的购物车列表集合，并且删除未登录的购物车列表集合
            if(cartInfoNoLoginList!=null&&cartInfoNoLoginList.size()>0){
                cartService.mergeToCartList(cartInfoNoLoginList,userId);
                cartService.deleteCartList(userTempId);
            }
        }
        //重定向到结算页面
        return "redirect://trade.gmall.com/trade";
    }
}
