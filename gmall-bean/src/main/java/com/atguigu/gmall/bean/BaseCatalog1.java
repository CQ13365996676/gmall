package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @Description 一级分类
 * @auther CQ
 * @create 2019-12-27 下午 4:25
 */
@Data
public class BaseCatalog1 implements Serializable {

    @Id
    @Column
    private String id;

    @Column
    private String name;

}
