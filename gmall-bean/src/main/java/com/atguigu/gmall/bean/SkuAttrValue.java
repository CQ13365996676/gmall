package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-30 下午 7:03
 */
@Data
public class SkuAttrValue implements Serializable {
    @Id
    @Column
    String id;

    @Column
    String attrId;

    @Column
    String valueId;

    @Column
    String skuId;
}
