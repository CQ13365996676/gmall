package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @Description 消费者监听器（利用注解实现ActiveMQ消费者监听器）
 * @auther CQ
 * @create 2020-01-13 下午 9:53
 */
@Component
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    /**
     * 利用注解实现ActiveMQ消费者监听器
     * 支付成功后更改订单状态的消息队列
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        //1.判断获取的消息是否为空
        if(!StringUtils.isEmpty(mapMessage)){
            String result = mapMessage.getString("result");
            //2.判断是否支付成功
            if("success".equals(result)){
                String orderId = mapMessage.getString("orderId");
                //3.改变订单状态
                orderService.updateOrderStatus(orderId);
                //4.通知库存系统减少库存
                orderService.sendOrderStatus(orderId);
            }
        }
    }

    /**
     * 库存减完成功后更改订单状态的消息队列
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        if(!StringUtils.isEmpty(mapMessage)){
            String orderId = mapMessage.getString("orderId");
            String status = mapMessage.getString("status");
            if("DEDUCTED".equals(status)){
                //说明减库存成功,将订单状态改为待发货
                orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
            }else{
                //说明减库存失败,将订单状态改为库存异常
                orderService.updateOrderStatus(orderId,ProcessStatus.STOCK_EXCEPTION);
            }
        }
    }

    /**
     * 延迟队列，24小时之后的订单状态检查
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void checkQueue(MapMessage mapMessage) throws JMSException {
        if(!StringUtils.isEmpty(mapMessage)){
            String orderId = mapMessage.getString("orderId");
            //查询订单是否已经支付成功
            boolean flag = paymentService.checkPayment(orderId);
            //如果失败则关闭订单
            if(!flag){
                orderService.updateOrderStatus(orderId, ProcessStatus.CLOSED);
            }
        }
    }

}
