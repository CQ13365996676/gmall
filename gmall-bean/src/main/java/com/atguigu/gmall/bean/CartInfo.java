package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-08 下午 5:37
 */
@Data
public class CartInfo implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column
    String id;

    @Column
    String userId;

    @Column
    String skuId;

    @Column
    BigDecimal cartPrice;

    @Column
    Integer skuNum;

    @Column
    String imgUrl;

    @Column
    String skuName;

    @Column
    String isChecked="1";

    // 实时价格
    @Transient
    BigDecimal skuPrice;

}
