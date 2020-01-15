package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.api.AlipayConstants.CHARSET_UTF8;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-12 下午 12:23
 */
@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @LoginRequire
    @RequestMapping("/index")
    public String index(String orderId, HttpServletRequest request){
        //根据订单ID获取需要支付的总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("orderId",orderId);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    /**
     * 通过支付宝进行支付
     * @param orderId
     * @return
     */
    @LoginRequire
    @ResponseBody
    @RequestMapping("/alipay/submit")
    public String alipaySubmit(String orderId, HttpServletResponse response){
        //0.做一个判断：支付日志中的订单支付状态 如果是已支付，则不生成二维码直接重定向到消息提示页面！（省略）

        //1.保存订单的支付信息，方便以后跟支付宝对账
        PaymentInfo paymentInfo = new PaymentInfo();

        //1.1初始化订单的支付信息（订单Id、对外交易编号、生成时间、交易总金额、交易状态默认为未交易、交易内容）
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject("过年买手机");

        //1.2将数据保存到数据库中
        paymentService.savePaymentInfo(paymentInfo);

        //2.返回支付宝支付的页面给用户进行支付
        //2.1获得初始化的AlipayClient（这里在配置文件直接将AlipayClient初始化并注入spring容器中）
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY,FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        //2.2创建API对应的request
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

        //2.3在公共参数中设置回跳和通知地址
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        //2.4填充业务参数（通过map封装业务参数,然后转换成JSON串）
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        alipayRequest.setBizContent(JSON.toJSONString(map));
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=" + CHARSET_UTF8);
        return form;
    }

    /**
     * 支付成功后的同步回调，跳转到自定义的页面
     * @return
     */
    @RequestMapping("/alipay/callback/return")
    public String callBack(){
        //跳转到订单页面
        return "redirect:"+AlipayConfig.return_order_url;
    }

    /**
     * 支付宝支付成功后的异步回调，需要验签并根据业务进行二次验签
     * @param paramsMap
     * @return
     * @throws AlipayApiException
     */
    @ResponseBody
    @RequestMapping("/alipay/callback/notify")
    public String callBackNotify(@RequestParam Map<String,String> paramsMap) throws AlipayApiException {
        //1.将异步通知中收到的所有参数都存放到map中（不适用request一个一个获取了，通过springmvc直接封装到map中）
        System.out.println("支付宝，你回来啦！");

        //2.调用SDK验证签名
        //2.1获取map中的总金额、第三方交易编号和用户的交易状态用于二次验证
        String totalAmountString = paramsMap.get("total_amount");
        BigDecimal totalAmount = new BigDecimal(totalAmountString);
        String outTradeNo = paramsMap.get("out_trade_no");
        String tradeStatus = paramsMap.get("trade_status");
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, CHARSET_UTF8, AlipayConfig.sign_type);

        // 3.验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
        if(signVerified){
            //3.1再次校验用户是否支付成功
            if("TRADE_SUCCESS".equals(tradeStatus)||"TRADE_FINISHED".equals(tradeStatus)){
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo);
                if(paymentInfo!=null){
                    //3.2验证支付单号是否已经成功交易或者已经关闭
                    if((PaymentStatus.PAID).equals(paymentInfo.getPaymentStatus())||(PaymentStatus.ClOSED).equals(paymentInfo.getPaymentStatus())){
                        return "failure";
                    }
                    //3.3验证总金额是否一致
                    if(paymentInfo.getTotalAmount().compareTo(totalAmount)!=0){
                        return "failure";
                    }
                    //3.4更新交易记录状态，改为付款，将支付宝交易号、回调时间存入数据库中
                    String tradeNo = paramsMap.get("trade_no");
                    PaymentInfo paymentInfoUpd = new PaymentInfo();
                    paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                    paymentInfoUpd.setCallbackTime(new Date());
                    paymentInfoUpd.setAlipayTradeNo(tradeNo);
                    paymentService.updatePaymentInfo(outTradeNo,paymentInfoUpd);
                    //3.5通知订单服务更改订单表中的支付状态（使用MQ中间件发送）
                    paymentService.sendPaymentResult(paymentInfo.getOrderId(),"success");
                    return "success";
                }else{
                    return "failure";
                }
            }
            return "failure";
        }else{
            // 3.6验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
    }

    /**
     * 根据订单Id退款（支付宝）
     * @throws AlipayApiException
     */
    @ResponseBody
    @RequestMapping("/refund")
    public String refund(String orderId){
        //通过返回值判断是否退款成功
        boolean flag = paymentService.refund(orderId);
        //判断完是否退款成功，再根据业务去实现业务逻辑
        if (flag){
            //业务逻辑
        }else{
            //业务逻辑
        }
        return ""+flag;
    }

}