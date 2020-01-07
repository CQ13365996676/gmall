package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-05 下午 2:41
 */
@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    /**
     * 通过三级分类ID、关键字、平台属性值查询商品信息
     * @param skuLsParams
     * @return
     */
    @RequestMapping("/list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        //0.设置每页显示的条数
        skuLsParams.setPageSize(1);
        //1.根据条件从ES中查询数据
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        //2.将查询所得的商品信息存放到request域中
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        request.setAttribute("skuLsInfoList",skuLsInfoList);
        //3.将查询所得的平台属性值封装成集合返回给前端
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        if(attrValueIdList != null && attrValueIdList.size() > 0){
            //3.1.根据平台属性值ID集合去查询所对应的的平台属性集合
            List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(attrValueIdList);
            //判断平台属性集合是否为空，不为空才能进入
            if(attrInfoList!=null&&attrInfoList.size()>0){
                //3.2.如果输入参数带有valueId的话，则需要将对应的平台属性在集合中删除
                //判断参数中是否有valueId，不为空才能进入
                if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                    //封装面包屑中的字符串
                    List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();
                    //遍历平台属性集合
                    for (Iterator<BaseAttrInfo> iterator = attrInfoList.iterator(); iterator.hasNext(); ) {
                        BaseAttrInfo baseAttrInfo = iterator.next();
                        //取出每一个平台属性对象的平台属性值集合
                        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                        //判断平台属性值集合是否为空，不为空才能进入
                        if(attrValueList!=null&&attrValueList.size()>0){
                            //遍历平台属性值集合
                            for (BaseAttrValue baseAttrValue : attrValueList) {
                                //将每一个平台属性值与输入参数中的valueId集合一一对比
                                for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                                    //如果有相同的话则将该平台属性在集合中删除
                                     if(skuLsParams.getValueId()[i].equals(baseAttrValue.getId())){
                                         iterator.remove();
                                         //需要将移除的平台属性与选中的平台属性值拼接成一个字符串
                                         //方案一:使用字符串封装，取值时直接使用
                                         //String selectedAttrInfo = baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName();
                                         //方案二:使用对象封装，取值时需要通过对象调用其属性值
                                         BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                                         baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());
                                         String urlParam = makeUrlParam(skuLsParams, skuLsParams.getValueId()[i]);
                                         baseAttrValueSelected.setUrlParam(urlParam);
                                         baseAttrValuesList.add(baseAttrValueSelected);
                                     }
                                }
                            }
                        }
                    }
                    //将面包屑集合对象存放到request域中
                    request.setAttribute("baseAttrValuesList",baseAttrValuesList);
                }
            }
            //3.3.将查询所得的平台属性与平台属性值集合存放到request域中
            request.setAttribute("attrInfoList",attrInfoList);
        }
        //4.需要将历史参数拼接成一个字符串返回给前端，为了用户点击平台属性时所需要的历史参数
        String urlParam = makeUrlParam(skuLsParams);
        request.setAttribute("urlParam",urlParam);
        //5.将输入的关键字返回给前端，用作面包屑
        String keyword = skuLsParams.getKeyword();
        request.setAttribute("keyword",keyword);
        //6.将查询的当前页数和总页数存放到request域中
        int pageNo = skuLsParams.getPageNo();
        request.setAttribute("pageNo",pageNo);
        long totalPages = skuLsResult.getTotalPages();
        request.setAttribute("totalPages",totalPages);
        return "list";
    }

    /**
     * 历史参数拼接
     * @param skuLsParams
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
        //判断关键字参数是否为空
        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            urlParam += "keyword="+skuLsParams.getKeyword();
        }
        //判断三级分类Id是否为空
        if(skuLsParams.getCatalog3Id()!=null&&skuLsParams.getCatalog3Id().length()>0){
            urlParam += "catalog3Id="+skuLsParams.getCatalog3Id();
        }
        //判断平台属性值集合是否为空
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            //遍历集合将valueId拼接到字符串中
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                //取出不需要添加的valueId，并判断两者是否相等
                if (excludeValueIds!=null&&excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        //结束本次循环
                        continue;
                    }
                }
                if(urlParam!=null&&urlParam.length()>0){
                    urlParam += "&";
                }
                urlParam += "valueId="+valueId;
            }
        }
        return urlParam;
    }

}
