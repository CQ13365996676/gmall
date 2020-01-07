package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

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
    @Override
    @Transactional
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

    /**
     * 根据三级分类ID查询商品的SPU信息
     * @param catalog3Id
     * @return
     */
    @Override
    public List<SpuInfo> getSpuInfoList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    /**
     * 获取基本销售属性集合
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    /**
     * 保存Spu信息
     * spu中包含了spu本身的信息，图片集合和销售属性集合
     * 销售属性集合包含了销售属性值集合
     * @param spuInfo
     */
    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        //1.将spu信息插入spu_info表，然后获取主键ID
        spuInfoMapper.insertSelective(spuInfo);
        //2.获取spuInfo中的spuImage集合，然后判断是否为空
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        String spuInfoId = spuInfo.getId();
        if(spuImageList != null && spuImageList.size() > 0){
            //3.若是不为空则遍历集合将spuId赋值给spuImage对象，然后插入spu_iamge表中
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfoId);
                spuImageMapper.insertSelective(spuImage);
            }
        }
        //4.获取spuInfo中的spuSaleAttr集合，然后判断是否为空
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList != null && spuSaleAttrList.size() > 0){
            //5.1若是不为空则遍历集合将spuId赋值给spuSaleAttr对象，然后插入spu_sale_attr表中
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfoId);
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                //5.2遍历spuSaleAttr集合时需要取出其中的spuSaleAttrValue集合，然后判断是否为空
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if(spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0){
                    //5.3若是不为空则遍历集合将spuId赋值给spuSaleAttrValue对象，然后插入spu_sale_attr_value表中
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfoId);
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    /**
     * 根据spuID获取对应的图片集合
     * @param spuId
     * @return
     */
    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    /**
     * 根据三级分类ID获取平台属性和对应的平台属性值
     * @param catalog3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    /**
     * 根据spuID获取销售属性集合
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    /**
     * 保存sku信息
     * @param skuInfo
     */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //1.向sku_info表中保存sku基本信息
        skuInfoMapper.insertSelective(skuInfo);
        //2.向sku_image表中保存sku的图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList != null && skuImageList.size() > 0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }
        //3.向sku_attr_value表中保存sku的平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if(skuAttrValueList != null && skuAttrValueList.size() > 0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        //4.向sku_sale_attr_value保存sku的销售属性
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    /**
     * 通过ID获取sku的信息（先去缓存中查询，若没有再去数据库中查找,为了处理高并发的问题，加锁）
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //方案一:使用redisson分布式锁
        return getSkuInfoByRedisson(skuId);
        //方案二:使用redis中的set分布式锁
        //return getSkuInfoByRedisSet(skuId);
    }

    /**
     * 通过ID获取sku信息（使用redisson分布式锁解决高并发下的缓存击穿）
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedisson(String skuId) {
        //先去查询缓存
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        try {
            jedis = redisUtil.getJedis();
            //判断缓存中是否有该key,若有则通过skuId获取redis中的sku信息
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            if(jedis.exists(skuKey)){
                String skuInfoJson = jedis.get(skuKey);
                //判断skuInfoJson是否为空,若不为空则转换成对象返回
                if(skuInfoJson != null){
                    skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                    return skuInfo;
                }else {
                    //如果为null则查询数据库
                    //查询数据库之前加锁
                    Config config = new Config();
                    config.useSingleServer().setAddress("redis://182.92.106.179:6379");
                    RedissonClient redisson = Redisson.create(config);
                    RLock lock = redisson.getLock("my-lock");
                    boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                    //上锁并判断是否上锁成功，若是成功则从数据库查询数据并插入到redis中
                    if(res){
                        try {
                            skuInfo = getSkuInfoDB(skuId);
                            if (skuInfo != null) {
                                // 将对象转换成Json字符串保存到redis中
                                String jsonString = JSON.toJSONString(skuInfo);
                                jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
                            }else{
                                jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                            }
                            return skuInfo;
                        } finally {
                            //解锁
                            lock.unlock();
                        }
                    }else{
                        return getSkuInfo(skuId);
                    }
                }
            }else{
                //若没有则从数据库中查询
                //查询数据库之前加锁
                Config config = new Config();
                config.useSingleServer().setAddress("redis://182.92.106.179:6379");
                RedissonClient redisson = Redisson.create(config);
                RLock lock = redisson.getLock("my-lock");
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                //上锁并判断是否上锁成功，若是成功则从数据库查询数据并插入到redis中
                if(res){
                    try {
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo != null) {
                            // 将对象转换成Json字符串保存到redis中
                            String jsonString = JSON.toJSONString(skuInfo);
                            jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
                        }else{
                            jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                        }
                        return skuInfo;
                    } finally {
                        //解锁
                        lock.unlock();
                    }
                }else{
                    return getSkuInfo(skuId);
                }
            }
        } catch (Exception e) {
            // 如果redis宕机了则从数据库中取得数据
            return getSkuInfoDB(skuId);
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
    }

    /**
     * 通过ID获取sku信息（使用redis中的set分布式锁解决高并发下的缓存击穿）
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByRedisSet(String skuId) {
        //先去缓存中查询数据
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        try {
            jedis = redisUtil.getJedis();
            //通过skuId获取redis中的sku信息
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            //如果redis中不存在这个key则从数据库查询并将数据插入到redis中，需要加锁
            if(!jedis.exists(skuKey)){
                //在查询数据库之前加锁
                //定义锁名与锁的value值
                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                String skuLockValue = UUID.randomUUID().toString().replaceAll("-","");
                //向redis中插入一个分布式锁（锁名 锁的值 NX:只在键不存在时，才对键进行设置操作 PX:过期时间单位为毫秒）
                String lockKey = jedis.set(skuLockKey, skuLockValue, "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                //判断lockKey是否为OK，若是则代表上锁成功，处理业务逻辑，最后解锁
                if("OK".equals(lockKey)){
                    // 从数据库中取得数据
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo != null) {
                        // 将对象转换成Json字符串保存到redis中
                        String jsonString = JSON.toJSONString(skuInfo);
                        jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
                    }else{
                        jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                    }
                    //使用Lua脚本进行解锁
                    String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(skuLockValue));
                    return skuInfo;
                }else{
                    //如果上锁不成功则代表已经有线程在占用，则进行等待或者重新调用自己
                    return getSkuInfo(skuId);
                }
            }else{
                String skuInfoJson = jedis.get(skuKey);
                //如果redis中这个key对应的value为空则从数据库查询并将数据插入到redis中，需要加锁
                if(StringUtils.isEmpty(skuInfoJson)){
                    //在查询数据库之前加锁
                    //定义锁名与锁的value值
                    String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                    String skuLockValue = UUID.randomUUID().toString().replaceAll("-","");
                    //向redis中插入一个分布式锁（锁名 锁的值 NX:只在键不存在时，才对键进行设置操作 PX:过期时间单位为毫秒）
                    String lockKey = jedis.set(skuLockKey, skuLockValue, "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                    //判断lockKey是否为OK，若是则代表上锁成功，处理业务逻辑，最后解锁
                    if("OK".equals(lockKey)){
                        // 从数据库中取得数据
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo != null) {
                            // 将对象转换成Json字符串保存到redis中
                            String jsonString = JSON.toJSONString(skuInfo);
                            jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
                        }else{
                            jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                        }
                        //使用Lua脚本进行解锁
                        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
                        jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(skuLockValue));
                        return skuInfo;
                    }else{
                        //如果上锁不成功则代表已经有线程在占用，则进行等待或者重新调用自己
                        return getSkuInfo(skuId);
                    }
                }else{
                    //将json串转换为skuInfo对象
                    skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                    return skuInfo;
                }
            }
        } catch (Exception e) {
            // 如果redis宕机了则从数据库中取得数据
            return getSkuInfoDB(skuId);
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
    }

    /**
     * 通过ID获取sku信息（没有解决高并发下的缓存击穿）
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoByNoHighConcurrence(String skuId) {
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        try {
            jedis = redisUtil.getJedis();
            //1.1.设置缓存中要查找的商品详情的Key
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            if(jedis.exists(skuKey)){
                //1.2.先去缓存中查找
                String skuInfoJson = jedis.get(skuKey);
                //3.判断数据是否存在
                if(!StringUtils.isEmpty(skuInfoJson)){
                    // 将Json字符串转换成SkuInfo对象
                    skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                }else{
                    // 从数据库中取得数据
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo != null) {
                        // 将对象转换成Json字符串保存到redis中
                        String jsonString = JSON.toJSONString(skuInfo);
                        jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
                    }else{
                        jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                    }
                }
            }else{
                // 从数据库中取得数据
                skuInfo = getSkuInfoDB(skuId);
                if (skuInfo != null) {
                    // 将对象转换成Json字符串保存到redis中
                    String jsonString = JSON.toJSONString(skuInfo);
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,jsonString);
                }else{
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                }
            }
        } catch (Exception e) {
            // 如果redis宕机了则从数据库中取得数据
            return getSkuInfoDB(skuId);
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return skuInfo;
    }

    /**
     * 通过ID获取sku的信息(走数据库)
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(String skuId) {
        //根据ID获取sku基本信息
        SkuInfo skuInfo = null;
        try {
            skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
            //根据SkuId获取sku图片信息
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(skuId);
            List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
            skuInfo.setSkuImageList(skuImageList);
            //根据SkuId获取平台属性值集合
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
            skuInfo.setSkuAttrValueList(skuAttrValueList);
            return skuInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return skuInfo;
    }

    /**
     * 根据spuID和skuId获取销售属性集合和销售属性值集合并判断是否被选中
     * @param skuInfo
     * @return
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getSpuId(),skuInfo.getId());
    }

    /**
     * 根据spuId查询该spu下的所有skuId及其销售属性值ID
     * @param spuId
     * @return
     */
    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    /**
     * 根据平台属性值ID集合去查询所对应的的平台属性集合
     * @param attrValueIdList
     * @return
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList) {
        //将集合转换成一个字符串
        String attrValueIds = org.apache.commons.lang3.StringUtils.join(attrValueIdList.toArray(), ",");
        return baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
    }

}
