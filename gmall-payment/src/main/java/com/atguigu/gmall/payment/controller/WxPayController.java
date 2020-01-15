package com.atguigu.gmall.payment.controller;

import com.atguigu.gmall.config.IdWorker;
import com.atguigu.gmall.config.StreamUtil;
import com.atguigu.gmall.service.PaymentService;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-13 下午 6:06
 */
@Controller
public class WxPayController {

    //微信的密钥
    @Value("${partnerkey}")
    private String partnerKey;
    
    @Autowired
    private PaymentService paymentService;

    /**
     * 生成微信支付的二维码（根据订单号 交易金额生成一个url_code）
     * @return
     */
    @ResponseBody
    @RequestMapping("/wx/submit")
    public Map<String,String> createNative(){
        //1.做一个判断：支付日志中的订单支付状态 如果是已支付，则不生成二维码直接重定向到消息提示页面！（省略）

        //2.根据订单号（自定义）和交易金额（测试直接使用1分）去生成url_code
        //2.1根据工具类生成一个不重复的订单ID（实际是前端传递过来的），交易金额同理
        IdWorker idWorker = new IdWorker();
        String orderId = idWorker.nextId()+"";
        Map<String,String> map = paymentService.createNative(orderId,1);

        //3.将存放二维码连接的map中返回给前端
        return map;
    }

    /**
     * 微信支付成功后的异步回调，需要验签并根据业务进行多次验签
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/wxpay/callback/notify")
    public String callBackNotify(HttpServletRequest request) throws Exception {
        System.out.println("微信，你回来啦");
        //1.微信以数据流形式将返回结果都放到流中了，所以获取数据流
        ServletInputStream inputStream = request.getInputStream();

        //2.利用工具类将流转换成字符串,再转换成map
        String xmlString = StreamUtil.inputStream2String(inputStream, "utf-8");
        System.out.println(xmlString);
        Map<String, String> map = WXPayUtil.xmlToMap(xmlString);

        //3.创建两个map，分别存放成功和失败的信息,然后转换成xml
        Map<String, String> successMap = new HashMap<>();
        successMap.put("return_code","SUCCESS");//必须有
        successMap.put("return_msg","OK");//可有可无
        String successXml = WXPayUtil.mapToXml(successMap);
        Map<String, String> failMap = new HashMap<>();
        failMap.put("return_code","FAIL");//必须有
        failMap.put("return_msg","FAIL");//可有可无
        String failXml = WXPayUtil.mapToXml(failMap);

        //4.基本验签
        boolean signatureValid = WXPayUtil.isSignatureValid(map, partnerKey);
        if(signatureValid){
            System.out.println("一次验签成功！");

            //5.二次验签，判断用户是否支付成功
            String returnCode = map.get("return_code");
            if(returnCode!=null && "SUCCESS".equals(returnCode)){
                System.out.println("二次验签成功！");

                //6.三次验签，例如交易金额和订单号是否一致等等业务逻辑......
                return successXml;
            }else{
                return failXml;
            }
        }else{
            return failXml;
        }
    }

}
