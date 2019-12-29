package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-27 下午 4:11
 */
@CrossOrigin
@RestController
public class ManageController {

    @Reference
    private ManageService manageService;

    /**
     * 获取一级分类集合
     * http://localhost:8082/getCatalog1
     * @return
     */
    @RequestMapping("/getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }

    /**
     * 根据一级分类ID获取二级分类集合
     * http://localhost:8082/getCatalog2?catalog1Id=4
     * @param baseCatalog2
     * @return
     */
    @RequestMapping("/getCatalog2")
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2){
        return manageService.getCatalog2(baseCatalog2);
    }

    /**
     * 根据二级分类ID获取三级分类集合
     * http://localhost:8082/getCatalog3?catalog2Id=13
     * @param baseCatalog3
     * @return
     */
    @RequestMapping("/getCatalog3")
    public List<BaseCatalog3> getCatalog2(BaseCatalog3 baseCatalog3){
        return manageService.getCatalog3(baseCatalog3);
    }

    /**
     * 根据三级分类ID获取平台属性集合
     * http://localhost:8082/attrInfoList?catalog3Id=324
     * @param baseAttrInfo
     * @return
     */
    @RequestMapping("/attrInfoList")
    public List<BaseAttrInfo> attrInfoList(BaseAttrInfo baseAttrInfo){
        return manageService.getAttrInfoList(baseAttrInfo);
    }

    /**
     * 添加/修改平台属性值
     * http://localhost:8082/saveAttrInfo
     * @param baseAttrInfo
     */
    @RequestMapping("/saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        if(baseAttrInfo != null){
            manageService.saveAttrInfo(baseAttrInfo);
        }
    }

    /**
     * 根据平台属性ID查询平台属性值集合
     * http://localhost:8082/getAttrValueList?attrId=100
     * @param baseAttrValue
     */
    @RequestMapping("/getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(BaseAttrValue baseAttrValue){
       return manageService.getAttrValueList(baseAttrValue);
    }


}
