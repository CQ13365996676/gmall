package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-05 下午 2:08
 */
@Data
public class SkuLsParams implements Serializable {

    String  keyword;

    String catalog3Id;

    String[] valueId;

    int pageNo=1;

    int pageSize=20;

}
