package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.config.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 购物车业务层实现类
 * @auther CQ
 * @create 2020-01-08 下午 5:46
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    /**
     * 将商品添加到购物车中
     * @param skuId
     * @param userId
     * @param skuNum
     */
    @Override
    public void addToCart(String skuId, String userId, String skuNum) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String cartKey =  CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CART_KEY_SUFFIX;
            //0.判断redis中是否有该key，为了不出现数据错误，如果没有该key则需要再次导入数据到redis中
            if(!jedis.exists(cartKey)){
                loadCartCache(userId);
            }
            //1.先将数据保存到数据库中
            //1.1根据skuId和userId判断是否有该商品记录
            Example example = new Example(CartInfo.class);
            example.createCriteria().andEqualTo("skuId",skuId).andEqualTo("userId",userId);
            CartInfo cartInfo = cartInfoMapper.selectOneByExample(example);
            //1.2根据skuId查询skuInfo表
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //1.3如果存在该商品记录则更新skuNum
            if(cartInfo!=null){
                cartInfo.setSkuNum(Integer.parseInt(skuNum)+cartInfo.getSkuNum());
                //更新实时价格
                cartInfo.setSkuPrice(skuInfo.getPrice());
                cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
            }else{
                //1.4如果没有该商品记录则赋值，最后向cartInfo表插入数据
                CartInfo cartInfo1 = new CartInfo();
                cartInfo1.setSkuPrice(skuInfo.getPrice());
                cartInfo1.setCartPrice(skuInfo.getPrice());
                cartInfo1.setSkuNum(Integer.parseInt(skuNum));
                cartInfo1.setSkuId(skuId);
                cartInfo1.setUserId(userId);
                cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
                cartInfo1.setSkuName(skuInfo.getSkuName());
                cartInfo = cartInfo1;
                cartInfoMapper.insertSelective(cartInfo);
            }
            //2.1将数据保存/更新到redis中
            String jsonString = JSON.toJSONString(cartInfo);
            jedis.hset(cartKey,skuId,jsonString);
            //2.2设置购物车数据失效的时间
            setCartkeyExpireTime(userId, jedis, cartKey);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
    }

    /**
     * 查询购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //1.先从redis中查询
        Jedis jedis = redisUtil.getJedis();
        String cartKey =  CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CART_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartKey);
        //如果有的话，遍历集合，将Json串转化成cartInfo对象
        if(stringList!=null&&stringList.size()>0){
            for (String cartJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //根据Id排序（实际根据修改时间倒序）
            cartInfoList.sort((CartInfo o1, CartInfo o2) -> o1.getId().compareTo(o2.getId()));
            return cartInfoList;
        }else{
            //2.如果redis中没有，则从数据库中查询，然后将数据保存到redis中
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    /**
     * 合并已登录和未登录的购物车列表集合
     * @param cartInfoNoLoginList
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
        //1.根据userId获取数据库中已登录的购物车列表
        List<CartInfo> cartInfoList = getCartList(userId);
        //2.判断已登录的购物车列表是否为空，如果不为空，合并两个集合
        if(cartInfoList!=null && cartInfoList.size()>0){
            for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
                //用于判断是否未登录的集合与已登录的集合中是否有相同的数据
                boolean isMatch = false;
                for (CartInfo cartInfo : cartInfoList) {
                    //根据两个对象的skuId判断已登录是否有未登录的这条数据，如果有则将skuNum相加，然后添加到数据库中
                    if(cartInfo.getSkuId().equals(cartInfoNoLogin.getSkuId())){
                        cartInfo.setSkuNum(cartInfo.getSkuNum()+cartInfoNoLogin.getSkuNum());
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
                        isMatch = true;
                    }
                }
                //如果没有相同的话则直接将该未登录的数据添加至已登录用户中
                if(!isMatch){
                    cartInfoNoLogin.setId(null);
                    cartInfoNoLogin.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoNoLogin);
                }
            }
        }else{
            //3.如果为空则直接将未登录的集合遍历添加到已登录的用户的表中
            for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
                cartInfoNoLogin.setId(null);
                cartInfoNoLogin.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoNoLogin);
            }
        }
        //4.更新缓存
        cartInfoList = loadCartCache(userId);
        //5.合并完还需要根据产品经理或者项目经理的需求合并勾选状态的数据（京东根据未登录的勾选状态而定）
        if(cartInfoList!=null && cartInfoList.size()>0){
            for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
                for (CartInfo cartInfo : cartInfoList) {
                    //如果未登录的某件商品存在于已登录的商品列表中则判断未登录的状态
                    if(cartInfoNoLogin.getSkuId().equals(cartInfo.getSkuId())){
                        //如果未登录的状态是已勾选，则判断已登录的是否为勾选状态
                        if("1".equals(cartInfoNoLogin.getIsChecked())){
                            //如果为未勾选状态则修改为已勾选状态
                            if("0".equals(cartInfo.getIsChecked())){
                                cartInfo.setIsChecked("1");
                                //调用更新状态的方法（包含了更新缓存的操作，所以与第4步不冲突）
                                checkCart(cartInfo.getSkuId(),"1",userId);
                            }
                        }
                    }
                }
            }
        }
        return cartInfoList;
    }

    /**
     * 删除未登录的购物车列表集合
     * @param userTempId
     */
    @Override
    public void deleteCartList(String userTempId) {
        //1.根据userId删除数据库中的数据
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userTempId);
        cartInfoMapper.deleteByExample(example);
        //2.根据userId删除redis中的数据
        Jedis jedis = redisUtil.getJedis();
        String cartKey =  CartConst.USER_KEY_PREFIX + userTempId +CartConst.USER_CART_KEY_SUFFIX;
        jedis.del(cartKey);
        jedis.close();
    }

    /**
     * 根据userId和skuId更改列表数据的状态（选中还是未选中）
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        //两种方案，这里采用第一种
        //一、先修改数据库中的数据，然后在修改redis中的数据
        //二、先修改数据库中的数据，然后删除缓存中原先的数据，然后在将新数据添加到缓存中（避免脏数据）
        //1.更新数据库中的状态
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        cartInfoMapper.updateByExampleSelective(cartInfo,example);
        //2.更新缓存中的状态
        Jedis jedis = redisUtil.getJedis();
        String cartKey =  CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CART_KEY_SUFFIX;
        //2.1将redis中原先的数据取出来转换成对象,赋值
        String cartInfoJson = jedis.hget(cartKey, skuId);
        CartInfo cartInfoUpdate = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfoUpdate.setIsChecked(isChecked);
        //2.2转换为Json串插入redis中
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoUpdate));
        jedis.close();
    }

    /**
     * 根据用户Id查询已勾选的购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //0.用于封装购物车列表数据的集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        //1.根据userId从redis中获取对应的购物车列表集合
        Jedis jedis = redisUtil.getJedis();
        String cartKey =  CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CART_KEY_SUFFIX;
        List<String> stringList = jedis.hvals(cartKey);
        //2.遍历该集合并取出已勾选状态的商品信息
        if(stringList!=null&&stringList.size()>0){
            for (String cartInfoJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                if("1".equals(cartInfo.getIsChecked())){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        jedis.close();
        return cartInfoList;
    }

    /**
     * 设置购物车数据失效的时间
     * @param userId
     * @param jedis
     * @param cartKey
     */
    private void setCartkeyExpireTime(String userId, Jedis jedis, String cartKey) {
        String userKey =  CartConst.USER_KEY_PREFIX + userId +CartConst.USERINFOKEY_SUFFIX;
        if(jedis.exists(userKey)){
            //用户在redis中存在的话，根据用户的过期时间设置该用户数据失效的时间
            Long time = jedis.ttl(userKey);
            jedis.expire(cartKey,time.intValue());
        }else{
            //用户在redis中不存在的话，则默认存放7天
            jedis.expire(cartKey,7*24*3600);
        }
    }

    /**
     * 更新缓存中的购物车列表信息（主要是实时价格），然后返回查询到的集合
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String cartKey =  CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CART_KEY_SUFFIX;
        //1.从数据库中查询购物车列表以及实时价格（skuInfo表中的price）
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        //2.判断该集合是否为空
        if(cartInfoList==null || cartInfoList.size()==0){
            return null;
        }else{
            //3.将数据保存到redis中
            Map<String,String> map = new HashMap<>();
            //将数据封装到map中，然后批量插入
            for (CartInfo cartInfo : cartInfoList) {
                map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
            }
            jedis.hmset(cartKey,map);
            setCartkeyExpireTime(userId,jedis,cartKey);
            jedis.close();
            return cartInfoList;
        }
    }
}
