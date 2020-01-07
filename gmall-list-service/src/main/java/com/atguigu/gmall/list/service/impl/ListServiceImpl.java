package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @auther CQ
 * @create 2020-01-05 下午 12:54
 */
@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    //ES中的逻辑数据库
    public static final String ES_INDEX="gmall";

    //ES中的逻辑表
    public static final String ES_TYPE="skuInfo";

    /**
     * 将商品信息保存到ES中（上架）
     * @param skuLsInfo
     */
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        //1.编写DSL语句（因为是插入语句所以省略，直接使用对象插入）
        //2.确定动作（此处为插入）
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        //3.交由jestClient执行
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过三级分类ID、关键字、平台属性值查询商品信息
     * @param skuLsParams
     * @return
     */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //1.编写DSL语句
        String query = makeQueryStringForSearch(skuLsParams);
        System.out.println(query);
        //2.确定执行动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        //3.交由jestClient执行
        try {
            SearchResult searchResult = jestClient.execute(search);
            //4.处理返回值
            SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);
            return skuLsResult;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 修改redis中对应商品的热度值
     * @param skuId
     */
    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        //每10次更新一次ES热度
        if(hotScore%10==0){
            updateHotScore(skuId, Math.round(hotScore));
        }
    }

    /**
     * 编写动态DSL语句
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //1.先构造searchSourceBuilder，相当于最外层的｛｝，然后再根据DSL语句慢慢往里添加
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //2.然后构造query（先过滤后查询）
        //2.1.因为query中包含bool，所以需要构造bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //2.2.因为bool中包含filter和must，所以需要根据参数构造filter和must
        //2.2.1.如果关键字有值，那么就构造must和设置高亮
        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length()>0){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            //设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            highlightBuilder.field("skuName");
            searchSourceBuilder.highlight(highlightBuilder);
        }
        //2.2.2.如果三级分类ID有值，那么就构造filter
        if(skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //2.2.3.如果平台属性值集合有值，那么就构造filter
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",skuLsParams.getValueId()[i]);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //2.3.将构造好的bool放入query中
        searchSourceBuilder.query(boolQueryBuilder);
        //3.构造sort（排序）
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //4.构造from size（分页）
        searchSourceBuilder.from((skuLsParams.getPageNo()-1)*skuLsParams.getPageSize());
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //5.构造aggs（聚合）
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);
        return searchSourceBuilder.toString();
    }

    /**
     * 处理DSL返回的结果集
     * @param searchResult
     * @return
     */
    private SkuLsResult makeResultForSearch(SearchResult searchResult,SkuLsParams skuLsParams) {
        if(searchResult != null){
            SkuLsResult skuLsResult = new SkuLsResult();
            //获取返回的SkuLsInfo对象集合
            List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
            if(hits != null && hits.size() > 0){
                List<SkuLsInfo> skuLsInfoList = new ArrayList();
                for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                    //获取hit中的对象信息
                    SkuLsInfo skuLsInfo = hit.source;
                    //获取hit中的高亮字段,并判断是否有值
                    Map<String, List<String>> highlight = hit.highlight;
                    if(highlight != null && highlight.size() > 0){
                        //只有一个字段，所以直接获取第一个即可，然后将高亮字段赋予对象
                        List<String> skuName = highlight.get("skuName");
                        skuLsInfo.setSkuName(skuName.get(0));
                    }
                    skuLsInfoList.add(skuLsInfo);
                }
                skuLsResult.setSkuLsInfoList(skuLsInfoList);
            }
            //获取查询到的所有平台属性值ID集合
            MetricAggregation aggregations = searchResult.getAggregations();
            TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
            if(groupby_attr != null){
                List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
                if(buckets != null && buckets.size() > 0){
                    List<String> attrValueIdList = new ArrayList<>();
                    for (TermsAggregation.Entry bucket : buckets) {
                        String valueId = bucket.getKey();
                        attrValueIdList.add(valueId);
                    }
                    skuLsResult.setAttrValueIdList(attrValueIdList);
                }
            }
            //获取搜索到的总条数
            Long total = searchResult.getTotal();
            skuLsResult.setTotal(total);
            //计算总页数
            //long totalPages = total%skuLsParams.getPageSize() == 0 ? total/skuLsParams.getPageSize() : total/skuLsParams.getPageSize()+1;
            long totalPage= (searchResult.getTotal() + skuLsParams.getPageSize() -1) / skuLsParams.getPageSize();
            skuLsResult.setTotalPages(totalPage);
            return skuLsResult;
        }else{
            return null;
        }
    }

    /**
     * 更新ES中gmall的skuInfo中的skuId的hotScore值
     * @param skuId
     * @param round
     */
    private void updateHotScore(String skuId, long round) {
        //编写DSL语句
        String update = "{\n" +
                "  \"doc\": {\"hotScore\":"+round+"}\n" +
                "}";
        //确定动作
        Update upt = new Update.Builder(update).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        //执行
        try {
            jestClient.execute(upt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
