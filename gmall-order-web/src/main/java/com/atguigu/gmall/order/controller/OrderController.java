package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.HttpClientUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-26 上午 10:09
 */
@Controller
public class OrderController {

    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @Reference
    private PaymentService paymentService;

    /**
     * 根据用户ID获取用户的所有地址（测试dubbo）
     * @param id
     * @return
     */
    @ResponseBody
    @GetMapping("/getAddressByUserId")
    public List<UserAddress> getAddressByUserId(String id){
        return userService.getAddressByUserId(id);
    }

    /**
     * 订单页面
     * @param request
     * @return
     */
    @LoginRequire
    @RequestMapping("/trade")
    public String trade(HttpServletRequest request){
        //1.从域中获取userId
        String userId = (String) request.getAttribute("userId");
        //2.根据用户Id查询用户地址
        List<UserAddress> userAddressList = userService.getAddressByUserId(userId);
        //3.根据用户Id查询已勾选的购物车列表
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        //3.1.将购物车列表信息循环封装到orderDetail集合中
        List<OrderDetail> orderDetailList = new ArrayList<>();
        if(cartInfoList!=null&&cartInfoList.size()>0){
            for (CartInfo cartInfo : cartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetailList.add(orderDetail);
            }
        }
        OrderInfo orderInfo = new OrderInfo();
        //3.2.将订单详情集合放入订单信息对象中，并让其计算总价格
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        //4.为了防止表单重复提交可以生成一个流水号放到隐藏域中用于表单提交校验
        String tradeNo = orderService.getTradeNo(userId);
        //5.将数据存放到域中
        request.setAttribute("userAddressList",userAddressList);
        //request.setAttribute("cartInfoList",cartInfoList);
        request.setAttribute("orderInfo",orderInfo);
        request.setAttribute("tradeNo",tradeNo);
        return "trade";
    }

    /**
     * 下单
     * @param orderInfo
     * @return
     */
    @LoginRequire
    @RequestMapping("/submitOrder")
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        //1.用户Id从request域中取得
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        //2.防止用户重复提交订单需要进行流水号验证，只有当验证通过时才能删除redis中的流水号
        String tradeNo = (String) request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId,tradeNo);
        if(!flag){
            request.setAttribute("errMsg","请勿重复提交订单！");
            return "tradeFail";
        }
        //3.校验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //3.1远程调用库存系统校验是否有库存
            String url = "http://www.gware.com/hasStock?skuId="+orderDetail.getSkuId()+"&num="+orderDetail.getSkuNum();
            String result = HttpClientUtil.doGet(url);
            //3.2如果没有库存则返回失败页面
            if("0".equals(result)){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"库存不足，请联系客服！");
                return "tradeFail";
            }
            //4.校验价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());
            //如果价格不同则需要刷新一下购物车列表的缓存并返回失败页面
            if(res!=0){
                //根据用户ID更新缓存
                cartService.loadCartCache(userId);
                request.setAttribute("errMsg",orderDetail.getSkuName()+"价格有变动，请联系客服重新下单！");
                return "tradeFail";
            }
        }
        //5.将订单信息保存到数据库中并返回订单号给支付宝页面
        String orderId = orderService.saveOrder(orderInfo);
        //6.删除redis中的流水号
        orderService.delTradeNo(userId);
        //7.利用延迟队列检查到期订单是否支付完成（正常时间是24小时，这里测试只用了15秒）
        paymentService.closeOrderInfo(orderId,15);
        //8.重定向到支付模块！
        return "redirect:http://payment.gmall.com/index?orderId="+orderId;
    }

    /**
     * 拆单接口
     * @return
     */
    @ResponseBody
    @RequestMapping("/orderSplit")
    public String orderSplit(String orderId,String wareSkuMap){
        //根据订单Id和仓库编号与商品的对照关系进行拆单并返回JSON串
        String wareMapListJson = orderService.splitOrder(orderId,wareSkuMap);
        return wareMapListJson;
    }

}
