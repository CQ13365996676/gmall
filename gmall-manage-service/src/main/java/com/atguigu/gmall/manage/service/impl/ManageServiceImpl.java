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

}
