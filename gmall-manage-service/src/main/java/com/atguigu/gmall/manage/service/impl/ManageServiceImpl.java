package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-27 下午 4:24
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    /**
     * 获取一级分类集合
     * @return
     */
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    /**
     * 根据一级分类ID获取二级分类集合
     * @param baseCatalog2
     * @return
     */
    @Override
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2) {
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    /**
     * 根据二级分类ID获取三级分类集合
     * @param baseCatalog3
     * @return
     */
    @Override
    public List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3) {
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    /**
     * 根据三级分类ID获取平台属性集合
     * @param baseAttrInfo
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(BaseAttrInfo baseAttrInfo) {
        return baseAttrInfoMapper.select(baseAttrInfo);
    }

    /**
     * 添加/修改平台属性值，涉及到多表的写操作，所以需要开启事务
     * @param baseAttrInfo
     */
    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //1.1.判断该平台属性对象是否有ID，有则表明修改，然后将该平台属性的平台属性值删除
        if(baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
            BaseAttrValue baseAttrValue = new BaseAttrValue();
            baseAttrValue.setAttrId(baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValue);
        }else{
            //1.2.没有则表明添加，将平台属性添加到平台属性表
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //2.根据修改/添加后回显的ID向平台属性值表添加平台属性值集合
        String attrInfoId = baseAttrInfo.getId();
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //3.判断平台属性值集合是否为空
        if(attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(attrInfoId);
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    /**
     * 根据平台属性ID查询平台属性值集合
     * @param baseAttrValue
     * @return
     */
    @Override
    public List<BaseAttrValue> getAttrValueList(BaseAttrValue baseAttrValue) {
        return baseAttrValueMapper.select(baseAttrValue);
    }

    /**
     * 根据三级分类ID查询商品的SPU信息
     * @param catalog3Id
     * @return
     */
    @Override
    public List<SpuInfo> getSpuInfoList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    /**
     * 获取基本销售属性集合
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    /**
     * 保存Spu信息
     * spu中包含了spu本身的信息，图片集合和销售属性集合
     * 销售属性集合包含了销售属性值集合
     * @param spuInfo
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //1.将spu信息插入spu_info表，然后获取主键ID
        spuInfoMapper.insertSelective(spuInfo);
        //2.获取spuInfo中的spuImage集合，然后判断是否为空
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        String spuInfoId = spuInfo.getId();
        if(spuImageList != null && spuImageList.size() > 0){
            //3.若是不为空则遍历集合将spuId赋值给spuImage对象，然后插入spu_iamge表中
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfoId);
                spuImageMapper.insertSelective(spuImage);
            }
        }
        //4.获取spuInfo中的spuSaleAttr集合，然后判断是否为空
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList != null && spuSaleAttrList.size() > 0){
            //5.1若是不为空则遍历集合将spuId赋值给spuSaleAttr对象，然后插入spu_sale_attr表中
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfoId);
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                //5.2遍历spuSaleAttr集合时需要取出其中的spuSaleAttrValue集合，然后判断是否为空
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if(spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0){
                    //5.3若是不为空则遍历集合将spuId赋值给spuSaleAttrValue对象，然后插入spu_sale_attr_value表中
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfoId);
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

}
