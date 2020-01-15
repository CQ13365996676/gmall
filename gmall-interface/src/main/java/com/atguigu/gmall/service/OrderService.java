package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.enums.ProcessStatus;

public interface OrderService {

    /**
     * 将订单信息保存到数据库中并返回订单号给支付宝页面
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 校验流水号
     * @param userId
     * @param tradeNo
     * @return
     */
    boolean checkTradeCode(String userId, String tradeNo);

    /**
     * 删除流水号
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 根据ID获取订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 根据订单ID修改订单已支付状态
     * @param orderId
     */
    void updateOrderStatus(String orderId);

    /**
     * 根据订单ID和订单状态去修改订单状态
     * @param orderId
     */
    void updateOrderStatus(String orderId, ProcessStatus status);

    /**
     * 根据订单ID通知库存系统减少库存
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 根据订单Id和仓库编号与商品的对照关系进行拆单并返回JSON串
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    String splitOrder(String orderId, String wareSkuMap);

}
