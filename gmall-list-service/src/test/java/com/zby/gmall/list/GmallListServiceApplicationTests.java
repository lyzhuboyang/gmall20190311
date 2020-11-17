package com.zby.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Autowired
    JestClient jestClient;

    @Test
    public void testEs() throws IOException {
        /*
        1、定义dsl语句
        2、定义要执行的动作
        3、jestClient执行动作
        4、获取返回结果
         */
        // 1、定义dsl语句
        String query= "{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"name\": \"行动\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        //  2、定义要执行的动作
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

        // 3、jestClient执行动作
        SearchResult searchResult = jestClient.execute(search);

        // 4、获取返回结果
        List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
        for (SearchResult.Hit<Map, Void> hit : hits) {
            Map source = hit.source;
            String name = (String) source.get("name");
            System.out.println(name);
        }
    }

}
