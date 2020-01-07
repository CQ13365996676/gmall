package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.bean.SpuSaleAttrValue;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 商品详情页面的控制器
 * @auther CQ
 * @create 2020-01-02 下午 12:05
 */
@Controller
@CrossOrigin
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    /**
     * 查询商品详细页的信息
     * @param skuId
     * @param request
     * @return
     */
    @RequestMapping("/{skuId}.html")
    public String skuInfoPage(@PathVariable("skuId") String skuId,HttpServletRequest request){
        //1.根据ID获取sku基本信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //2.根据spuID和skuId获取销售属性集合和销售属性值集合并判断是否被选中
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        //3.1.根据spuId查询该spu下的所有skuId及其销售属性值ID
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        //3.2.将销售属性值集合遍历拼接成map集合
        Map<String, String> saleValueIdMap = new HashMap<>();
        String key = "";
        for (int i = 0 ; i < skuSaleAttrValueList.size(); i ++) {
            //判断value是否为空，不为空则需要加上'|'
            if(key != ""){
                key += "|";
            }
            //拼接销售属性值ID
            key += skuSaleAttrValueList.get(i).getSaleAttrValueId();
            //判断当前i+1是否与销售属性值集合的长度一样，若一样则重置并结束
            //判断下一个值的skuId是否与当前skuId一样，不一样则重置并结束
            if((i+1) == skuSaleAttrValueList.size() || !skuSaleAttrValueList.get(i).getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                saleValueIdMap.put(key,skuSaleAttrValueList.get(i).getSkuId());
                key = "";
            }
        }
        //3.3.将map转换成json串
        String valuesSkuJson = JSON.toJSONString(saleValueIdMap);
        //4.将数据保存到域中
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        request.setAttribute("valuesSkuJson",valuesSkuJson);
        //5.每访问一次商品详情页则增加1的热度
        listService.incrHotScore(skuId);
        return "item";
    }

}
