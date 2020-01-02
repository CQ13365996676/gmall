package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    /**
     * 根据spuId查询该spu下的所有skuId及其销售属性值ID
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);

}
