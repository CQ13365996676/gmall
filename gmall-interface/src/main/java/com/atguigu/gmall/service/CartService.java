package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

/**
 * 购物车接口
 */
public interface CartService {

    /**
     * 将商品添加到购物车中
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(String skuId, String userId, String skuNum);

    /**
     * 查询购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并已登录和未登录的购物车列表集合
     * @param cartInfoNoLoginList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId);

    /**
     * 删除未登录的购物车列表集合
     * @param userTempId
     */
    void deleteCartList(String userTempId);

    /**
     * 根据userId和skuId更改列表数据的状态（选中还是未选中）
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据用户Id查询已勾选的购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 更新缓存中的购物车列表信息（主要是实时价格）
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);

}
