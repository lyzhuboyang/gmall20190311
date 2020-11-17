package com.zby.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zby.gmall.bean.SkuLsInfo;
import com.zby.gmall.bean.SkuLsParams;
import com.zby.gmall.bean.SkuLsReslut;
import com.zby.gmall.config.RedisUtil;
import com.zby.gmall.service.ListService;
import io.searchbox.client.JestClient;

import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
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

@Service
public class ListServiceImpl implements ListService {
    //调用es客户端
    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    public static final String ES_INDEX="gmall";
    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        /**
         * 1、明确保存对象
         * 2、创建动作
         * 3、执行动作
         */
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检索
     * @param skuLsParams
     * @return
     */
    @Override
    public SkuLsReslut search(SkuLsParams skuLsParams) {
        /**
         * 1、定义dsl语句
         * 2、定义执行的动作
         * 3、执行动作
         * 4、获取返回结果
         */
        SearchResult searchResult = null;
        //1、动态生成dsl语句
        String query = makeQueryStringForSearch(skuLsParams);
        //2、定义执行的动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        //3、执行动作
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //4、获取返回结果，把kibana中查询的结果集封装成我们想要的数据
        //第一个参数：查询的结果集
        //第二个参数：用户输入参数查询的实体类，可以设置每页显示的数据条数
        SkuLsReslut skuLsReslut = makeResultForSearch(searchResult,skuLsParams);
        return skuLsReslut;
    }

    //更新商品热度
    @Override
    public void updateHotScore(String skuId) {
        /**
         * 1、更新次数放入redis
         * 2、当达到一定值时，更新es
         */
        Jedis jedis = redisUtil.getJedis();
        //使用那种数据类型以及key
        String hotKey = "hotScore";
        Double hotScore = jedis.zincrby(hotKey, 1, "skuId:" + skuId);
        //符合规则的时候，将数据放入es
        if(hotScore%10==0){
            // Math.round(11.5) = 12 Math.round(-11.5) = -11
            updateHotScore(skuId,Math.round(hotScore));
        }
    }

    //更新es
    private void updateHotScore(String skuId, long hotScore) {
         /*
        1.  定义dsl 语句
        2.  定义执行的动作
        3.  执行动作
         */
        //1.  定义dsl 语句
        String updEs = "{\n" +
                "  \"doc\": {\n" +
                "    \"doubanScore\":\""+hotScore+"\"\n" +
                "  }\n" +
                "}";
        // 2.  定义执行的动作
        Update update = new Update.Builder(updEs).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        //3.  执行动作
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //把kibana中查询的结果集封装成我们想要的数据
    private SkuLsReslut makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsReslut skuLsReslut = new SkuLsReslut();

        //dsl语句查询的结果有多个，hits:n,用list来接收
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            //获取SkuLsInfo
            SkuLsInfo skuLsInfo = hit.source;
            //hit.source中的skuName并不是高亮的
            if(hit.highlight!=null&&hit.highlight.size()>0){
                List<String> list = hit.highlight.get("skuName");
                String skuNameHI = list.get(0);
                //将原来的hitsource中的skuName换成高亮的
                skuLsInfo.setSkuName(skuNameHI);
            }
            skuLsInfoList.add(skuLsInfo);
        }

        skuLsReslut.setSkuLsInfoList(skuLsInfoList);
        skuLsReslut.setTotal(searchResult.getTotal());
        // 10  3| 4  9  3| 3
        //long tp= searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsReslut.setTotalPages(totalPages);

        //平台属性值Id
        List<String> attrValueIdList = new ArrayList<>();
        //不通过_source.skuAttrValueList.valueId的方式来给attrValueIdList添加valueId，_source.skuAttrValueList.valueId可能会重复，list是有序可重复的集合
        //所以通过聚合里面的valueId来向attrValueIdList添加ValueId
        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            //将valueId放入集合中
            attrValueIdList.add(valueId);
        }

        skuLsReslut.setAttrValueIdList(attrValueIdList);
        return skuLsReslut;
    }

    //动态生成dsl语句方法
    //以下代码要和kibana中的语句高度稳合
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //创建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //skuLsParams用户输入的条件
        //判断三级分类Id
        if(skuLsParams.getCatalog3Id()!=null &&skuLsParams.getCatalog3Id().length()>0){
            //term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            //filter -- term
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //平台属性值Id
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            //循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                //term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                //filter -- term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //判断keyword=skuName 检索
        if(skuLsParams.getKeyword()!=null&&skuLsParams.getKeyword().length()>0){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            //高亮必须有查询
            //设置高亮 高亮是和query同一等级
            //获取高亮对象
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            //设置高亮属性
            highlighter.preTags("<span style=color:red>");
            highlighter.postTags("</span>");
            highlighter.field("skuName");

            //将设置好的高亮对象放入查询器
            searchSourceBuilder.highlight(highlighter);
        }

        // query
        searchSourceBuilder.query(boolQueryBuilder);

        //设置分页
        // 从第几条数据开始显示
        // select * from skuInfo limit (pageNo-1)*pageSize ,pageSize
        // 3  , 2  |  0,2  | 2, 2
        //设置每页的起始下标
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        //设置每页的显示的条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //设置聚合
        // 将term 封装到agg ，按照skuAttrValueList.valueId 进行聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);
        String query = searchSourceBuilder.toString();
        //动态生成的sql语句
        System.out.println("query："+query);
        return query;
    }


}
