package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description SPU控制器
 * @auther CQ
 * @create 2019-12-28 下午 4:18
 */
@CrossOrigin
@RestController
public class SpuManageController {

    @Reference
    private ManageService manageService;

    /**
     * 根据三级分类ID查询商品的SPU信息
     * http://localhost:8082/spuList?catalog3Id=64
     * @param catalog3Id
     * @return
     */
    @RequestMapping("/spuList")
    public List<SpuInfo> getSpuInfoList(String catalog3Id){
        return manageService.getSpuInfoList(catalog3Id);
    }


    /**
     * 获取基本销售属性集合
     * http://localhost:8082/baseSaleAttrList
     * @return
     */
    @RequestMapping("/baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    /**
     * 保存Spu信息
     * http://localhost:8082/saveSpuInfo
     * @param spuInfo
     */
    @RequestMapping("/saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
    }
}
