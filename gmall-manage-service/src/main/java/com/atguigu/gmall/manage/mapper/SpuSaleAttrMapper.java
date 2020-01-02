package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    /**
     * 根据spuID获取销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    /**
     * 根据spuID和skuId获取销售属性集合和销售属性值集合并判断是否被选中
     * @param spuId
     * @param skuId
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String spuId,String skuId);

}
