package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

/**
 * ES模块接口
 */
public interface ListService {

    /**
     * 商品上架
     * @param skuLsInfo
     */
    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * 通过三级分类ID、关键字、平台属性值查询商品信息
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 修改redis中对应商品的热度值
     * @param skuId
     */
    void incrHotScore(String skuId);

}
