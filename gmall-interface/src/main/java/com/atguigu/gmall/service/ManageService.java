package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {
    /**
     * 获取一级分类集合
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类ID获取二级分类集合
     * @param baseCatalog2
     * @return
     */
    List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2);

    /**
     * 根据二级分类ID获取三级分类集合
     * @param baseCatalog3
     * @return
     */
    List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3);

    /**
     * 根据三级分类ID获取平台属性集合
     * @param baseAttrInfo
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(BaseAttrInfo baseAttrInfo);

    /**
     * 添加/修改平台属性值
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性ID查询平台属性值集合
     * @param baseAttrValue
     * @return
     */
    List<BaseAttrValue> getAttrValueList(BaseAttrValue baseAttrValue);


    /**
     * 根据三级分类ID查询商品的SPU信息
     * @param catalog3Id
     * @return
     */
    List<SpuInfo> getSpuInfoList(String catalog3Id);

    /**
     * 获取基本销售属性集合
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存Spu信息
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuID获取对应的图片集合
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(String spuId);

    /**
     * 根据三级分类ID获取平台属性和对应的平台属性值
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 根据spuID获取销售属性集合
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 保存sku信息
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 通过ID获取sku的基本信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据spuID和skuId获取销售属性集合和销售属性值集合并判断是否被选中
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId查询该spu下的所有skuId及其销售属性值ID
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

}
