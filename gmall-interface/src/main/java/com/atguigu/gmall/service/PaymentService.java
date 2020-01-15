package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    /**
     * 保存订单的支付信息
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据第三方交易编号查询订单的支付信息
     * @param outTradeNo
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo);

    /**
     * 根据第三方交易编号修改订单的支付信息
     * @param outTradeNo
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfoUpd);

    /**
     * 根据订单Id退款（支付宝）
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 根据订单号 交易金额生成一个url_code（微信支付的二维码）
     * @param orderId
     * @param totalAmount
     * @return
     */
    Map<String, String> createNative(String orderId, Integer totalAmount);

    /**
     * 根据订单ID和支付结果去通知订单服务更改订单表的状态
     * @param orderId
     * @param result
     */
    void sendPaymentResult(String orderId, String result);

    /**
     * 通过延迟队列实现根据时间和订单ID查询订单支付状态并判断是否要删除订单
     * @param orderId
     * @param second
     */
    void closeOrderInfo(String orderId, Integer second);

    /**
     * 根据订单ID查询订单然后判断是否在支付宝上支付完成
     * @param orderId
     * @return
     */
    boolean checkPayment(String orderId);
}
