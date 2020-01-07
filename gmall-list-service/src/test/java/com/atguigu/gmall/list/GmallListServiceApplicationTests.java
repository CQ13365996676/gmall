package com.atguigu.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    @Autowired
    private JestClient jestClient;

    /**
     * ES测试
     */
    @Test
    public void contextLoads() {
        //1.编写DSL语句
        String query = "{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"actorList.name\": \"张涵予\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        //2.确定执行的动作（此时为查询）并执行DSL语句
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();
        try {
            SearchResult searchResult = jestClient.execute(search);
            //3.处理返回集
            List<SearchResult.Hit<HashMap, Void>> hits = searchResult.getHits(HashMap.class);
            for (SearchResult.Hit<HashMap, Void> hit : hits) {
                HashMap source = hit.source;
                System.out.println(source);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
