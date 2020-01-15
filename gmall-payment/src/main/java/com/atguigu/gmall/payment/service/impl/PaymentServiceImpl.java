package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.HttpClient;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-12 下午 1:56
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    //微信的应用ID
    @Value("${appid}")
    private String appId;

    //微信的商户号Id
    @Value("${partner}")
    private String partner;

    //微信的密钥
    @Value("${partnerkey}")
    private String partnerKey;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    /**
     * 保存订单的支付信息
     * @param paymentInfo
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    /**
     * 根据第三方交易编号查询订单的支付信息
     * @param outTradeNo
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfo);
        return paymentInfoQuery;
    }

    /**
     * 根据第三方交易编号修改订单的支付信息
     * @param outTradeNo
     * @param paymentInfoUpd
     */
    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfoUpd) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",outTradeNo);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUpd,example);
    }

    /**
     * 根据订单Id退款（支付宝）
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(String orderId){
        //1.根据订单ID查询订单的支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfo);
        //2.将需要退款的信息封装到map中
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());
        map.put("refund_amount",paymentInfoQuery.getTotalAmount());
        map.put("refund_reason","过年没钱了");
        //3.调用支付宝的退款接口
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizContent(JSON.toJSONString(map));
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            //4.判断退款是否成功
            if(response.isSuccess()){
                //更改订单表中的支付状态为关闭
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.ClOSED);
                updatePaymentInfo(paymentInfoQuery.getOutTradeNo(),paymentInfoUpd);
                return true;
            } else {
                return false;
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据订单号 交易金额生成一个url_code（微信支付的二维码）
     * @param orderId
     * @param totalAmount
     * @return
     */
    @Override
    public Map<String, String> createNative(String orderId, Integer totalAmount) {
        //1.将微信支付需要的参数封装到map中（签名即秘钥不需要封装到参数中，直接通过工具类去生成参数xml）
        Map<String, String> param = new HashMap<>();
        param.put("appid",appId);
        param.put("mch_id",partner);
        param.put("nonce_str", WXPayUtil.generateNonceStr());
        param.put("body","DNF充值");
        param.put("out_trade_no",orderId);
        param.put("total_fee",totalAmount+"");
        param.put("spbill_create_ip","127.0.0.1");
        param.put("notify_url","http://2q86330w26.picp.vip/wxpay/callback/notify");
        param.put("trade_type","NATIVE");
        try {
            //2.通过WXPayUtil工具类（SDK自带的）转换成xml
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerKey);
            //3.利用工具类向微信发送请求（https://api.mch.weixin.qq.com/pay/unifiedorder）
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setXmlParam(xmlParam);
            httpClient.setHttps(true);
            httpClient.post();
            //4.获取返回结果，因为返回结果是以xml形式返回，所以得通过工具类将xml转换成map
            String result = httpClient.getContent();
            System.out.println(result);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
            //5.将二维码链接返回封装到map中
            Map<String, String> map = new HashMap<>();
            String codeUrl = resultMap.get("code_url");
            map.put("code_url",codeUrl);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据订单ID和支付结果去通知订单服务更改订单表的状态
     * @param orderId
     * @param result
     */
    @Override
    public void sendPaymentResult(String orderId, String result) {
        //1.获取ActiveMQ连接并将连接开启
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //2.根据connection获取session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //3.根据session创建队列
            Queue queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //4.根据session创建生产者
            MessageProducer producer = session.createProducer(queue);
            //5.设置要发送的消息
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId",orderId);
            mapMessage.setString("result",result);
            //6.发送消息
            producer.send(mapMessage);
            //7.提交事务并关闭各个连接
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过延迟队列实现根据时间和订单ID查询订单支付状态并判断是否要删除订单
     * @param orderId
     * @param second
     */
    @Override
    public void closeOrderInfo(String orderId, Integer second) {
        //1.获取并开启连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //2.根据连接创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //3.根据session创建队列
            Queue queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            //4.根据session创建提供者
            MessageProducer producer = session.createProducer(queue);
            //5.创建要发送的消息类型与数据
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId",orderId);
            //6.设置延迟队列的延迟时间并发送消息
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,second*1000);
            producer.send(mapMessage);
            //7.提交session并关闭所有连接
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据订单ID查询订单是否在支付宝上已经支付了
     * @param orderId
     * @return
     */
    @Override
    public boolean checkPayment(String orderId) {
        //1.根据订单ID查询订单的第三方交易单号
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        if(orderInfo!=null){
            String outTradeNo = orderInfo.getOutTradeNo();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, String> map = new HashMap<>();
            map.put("out_trade_no",outTradeNo);
            request.setBizContent(JSON.toJSONString(map));
            AlipayTradeQueryResponse response = null;
            try {
                response = alipayClient.execute(request);
                //2.根据响应的结果去实现业务逻辑
                if(response.isSuccess()){
                    String tradeStatus = response.getTradeStatus();
                    if("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                        return true;
                    }else{
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (AlipayApiException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

}
