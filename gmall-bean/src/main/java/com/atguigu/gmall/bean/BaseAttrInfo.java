package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-27 下午 4:27
 */
@Data
public class BaseAttrInfo implements Serializable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)//主键回显
    private String id;

    @Column
    private String attrName;

    @Column
    private String catalog3Id;

    //不是数据库的属性，但是业务逻辑需要，加上@Transient注解
    @Transient
    private List<BaseAttrValue> attrValueList;

}
