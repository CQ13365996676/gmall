package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2019-12-30 下午 4:38
 */
@CrossOrigin
@RestController
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    /**
     * 根据spuID获取对应的图片集合
     * http://localhost:8082/spuImageList?spuId=76
     * @param spuId
     * @return
     */
    @RequestMapping("/spuImageList")
    public List<SpuImage> spuImageList(String spuId){
        return manageService.getSpuImageList(spuId);
    }

    /**
     * 根据spuID获取销售属性集合
     * http://localhost:8082/spuSaleAttrList?spuId=76
     * @param spuId
     * @return
     */
    @RequestMapping("/spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
       return manageService.getSpuSaleAttrList(spuId);
    }

    /**
     * 保存sku信息
     * http://localhost:8082/saveSkuInfo
     * @param skuInfo
     */
    @RequestMapping("/saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        if(skuInfo != null){
            manageService.saveSkuInfo(skuInfo);
        }
    }

    /**
     * 上架商品（将数据库中的数据保存到ES中）
     * @param skuId
     */
    @RequestMapping("/onSale")
    public void onSale(String skuId){
        //1.获取skuInfo信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //2.将skuInfo信息拷贝到skuLsInfo中
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        BeanUtils.copyProperties(skuInfo,skuLsInfo);
        //3.调用ES模块中的商家信息
        listService.saveSkuInfo(skuLsInfo);
    }

}
