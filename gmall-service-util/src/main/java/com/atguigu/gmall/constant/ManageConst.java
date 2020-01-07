package com.atguigu.gmall.constant;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-03 下午 8:25
 */
public class ManageConst {

    //商品详情的前缀
    public static final String SKUKEY_PREFIX="sku:";

    //商品详情的后缀
    public static final String SKUKEY_SUFFIX=":info";

    //商品详情的过期时间
    public static final int SKUKEY_TIMEOUT=7*24*60*60;

    //分布式锁的失效时间
    public static final int SKULOCK_EXPIRE_PX=10000;

    //分布式锁的后缀名
    public static final String SKULOCK_SUFFIX=":lock";

}
