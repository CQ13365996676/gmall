package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-28 下午 4:15
 */
@Data
public class SpuInfo implements Serializable {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String spuName;

    @Column
    private String description;

    @Column
    private String catalog3Id;

    @Transient
    private List<SpuImage> spuImageList;

    @Transient
    private List<SpuSaleAttr> spuSaleAttrList;
}
