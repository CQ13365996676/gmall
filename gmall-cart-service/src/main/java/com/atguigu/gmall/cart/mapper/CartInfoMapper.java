package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {

    /**
     * 根据userId查询购物车列表以及实时价格（skuInfo表中的price）
     * @param userId
     * @return
     */
    List<CartInfo> selectCartListWithCurPrice(String userId);

}
