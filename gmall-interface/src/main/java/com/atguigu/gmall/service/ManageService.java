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

}
