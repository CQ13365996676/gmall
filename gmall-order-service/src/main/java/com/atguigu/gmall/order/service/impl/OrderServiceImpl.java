package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-11 下午 6:19
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    /**
     * 将订单信息分别保存到数据库中的两张表内，并返回订单号给支付宝页面
     * @param orderInfo
     * @return
     */
    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //1.初始化orderInfo中的信息
        //总价格和创建时间
        orderInfo.sumTotalAmount();
        orderInfo.setCreateTime(new Date());
        //订单进度状态、订单状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //第三方支付编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 过期时间24小时！
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //2.将orderInfo信息插入表中
        orderInfoMapper.insertSelective(orderInfo);
        String orderInfoId = orderInfo.getId();
        //3.遍历详情集合，依次插入数据到表中
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null&&orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                //初始化orderDetail中的信息，然后插入数据库中
                orderDetail.setId(null);
                orderDetail.setOrderId(orderInfoId);
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
        return orderInfoId;
    }

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        //1.根据userId创建一个存放流水号的key（在redis中）
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        //2.利用UUID生成流水号
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        //3.保存在redis中
        jedis.set(tradeNoKey,tradeNo);
        jedis.close();
        return tradeNo;
    }

    /**
     * 校验流水号
     * @param userId
     * @param tradeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeNo) {
        //1.根据用户Id查询缓存中的流水号
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeNoRedis = jedis.get(tradeNoKey);
        jedis.close();
        //2.判断两者是否一致
        if(tradeNo!=null&&tradeNo.equals(tradeNoRedis)){
            return true;
        }
        return false;
    }

    /**
     * 删除流水号
     * @param userId
     */
    @Override
    public void delTradeNo(String userId) {
        //1.根据用户Id删除redis中的流水号
        Jedis jedis = redisUtil.getJedis();
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode = jedis.get(tradeNoKey);
        String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedis.eval(script, Collections.singletonList(tradeNoKey),Collections.singletonList(tradeCode));
        jedis.close();
    }

    /**
     * 根据ID获取订单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    /**
     * 根据订单ID修改订单的已支付状态
     * @param orderId
     */
    @Override
    public void updateOrderStatus(String orderId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(ProcessStatus.PAID);
        orderInfo.setOrderStatus(ProcessStatus.PAID.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    /**
     * 根据订单ID和订单状态去修改订单状态
     * @param orderId
     * @param status
     */
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus status) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(status);
        orderInfo.setOrderStatus(status.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    /**
     * 根据订单ID通知库存系统减少库存
     * @param orderId
     */
    @Override
    public void sendOrderStatus(String orderId) {
        //1.获取并打开ActiveMQ连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //2.根据connection创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //3.根据session创建队列
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            //4.根据session创建消息提供者
            MessageProducer producer = session.createProducer(queue);
            //5.创建需要发送的消息并发送，调用initWareOrder方法（根据orderId返回需要的JSON）
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
            String orderJson = initWareOrder(orderId);
            textMessage.setText(orderJson);
            producer.send(textMessage);
            //6.提交事务并关闭连接
            session.commit();
            session.close();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据订单Id和仓库编号与商品的对照关系进行拆单并返回JSON串
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public String splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList  = new ArrayList<>();
        //1.根据订单ID查询订单信息
        OrderInfo orderInfo = getOrderInfo(orderId);
        //2.将仓库编号与商品的对照关系由JSON串转换成List<Map>
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        //3.获取订单的详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null&&orderDetailList.size()>0){
            //4.遍历关系集合将需要添加的信息保存到数据库中
            for (Map map : maps) {
                //获取仓库ID（有多少个仓库ID就生成多少子订单）
                String wareId = (String) map.get("wareId");
                OrderInfo subOrderInfo = new OrderInfo();
                BeanUtils.copyProperties(orderInfo,subOrderInfo);
                subOrderInfo.setId(null);
                subOrderInfo.setWareId(wareId);
                subOrderInfo.setParentOrderId(orderId);

                //根据skuId遍历出子订单的详细信息
                List<OrderDetail> subOrderDetailList  = new ArrayList<>();
                List<String> skuIds = (List<String>) map.get("skuIds");
                if(skuIds!=null && skuIds.size()>0){
                    for (String skuId : skuIds) {
                        for (OrderDetail orderDetail : orderDetailList) {
                            if(orderDetail.getSkuId().equals(skuId)){
                                orderDetail.setId(null);
                                subOrderDetailList .add(orderDetail);
                            }
                        }
                    }
                }
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                subOrderInfo.sumTotalAmount();

                //添加到数据库中并保存到子订单集合中
                saveOrder(subOrderInfo);
                subOrderInfoList.add(orderInfo);
            }
            //修改原订单状态为已拆单
            updateOrderStatus(orderId,ProcessStatus.SPLIT);
        }

        //4.将map转换成JSON串
        List<Map> list = new ArrayList<>();
        if(subOrderInfoList!=null && subOrderInfoList.size()>0){
            for (OrderInfo subOrderInfo : subOrderInfoList) {
                Map<String, Object> initWareOrder = initWareOrder(subOrderInfo);
                list.add(initWareOrder);
            }
            return JSON.toJSONString(list);
        }
        return null;
    }

    /**
     * 根据订单ID获取订单信息并生成相应的JSON参数返回
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        //1.根据订单ID将订单信息查询出来
        OrderInfo orderInfo = getOrderInfo(orderId);
        //2.将订单信息封装到map中
        Map<String,Object> map = initWareOrder(orderInfo);
        //3.将map转换成json串发送给消息中间件
        if(map != null && map.size() > 0){
            return JSON.toJSONString(map);
        }
        return null;
    }

    /**
     * 根据库存系统需要的参数将订单信息封装到map中
     * @param orderInfo
     * @return
     */
    private Map<String, Object> initWareOrder(OrderInfo orderInfo) {
        //1.将订单对象的数据封装到map中
        Map<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","过年买电脑！");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId());

        //2.获取并封装购买商品明细
        List<Map<String,Object>> detailList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(orderDetailList!=null&&orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                Map<String, Object> detailMap  = new HashMap<>();
                detailMap.put("skuId",orderDetail.getSkuId());
                detailMap.put("skuNum",orderDetail.getSkuNum());
                detailMap.put("skuName",orderDetail.getSkuName());
                detailList.add(detailMap);
            }
        }
        map.put("details",detailList);
        return map;
    }

}
