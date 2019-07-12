package ai.test.elasticsearch.controller;

import ai.test.elasticsearch.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by suman.das on 6/20/19.
 */
@RestController()
@RequestMapping("/users")
public class UserController {
    @Autowired
    private RestHighLevelClient client;

    @PostMapping("/index")
    public void index() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("users");
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 2)
        );
        Map<String, Object> message = new HashMap<>();
        message.put("type", "text");

        Map<String,Object> keyWordMap = new HashMap<>();
        Map<String,Object> keyWordValueMap = new HashMap<>();
        keyWordValueMap.put("type","keyword");
        keyWordValueMap.put("ignore_above",256);
        keyWordMap.put("keyword",keyWordValueMap);
        message.put("fields", keyWordMap);

        Map<String, Object> properties = new HashMap<>();
        properties.put("userId", message);
        properties.put("name", message);

        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", properties);
        request.mapping(mapping);

        GetIndexRequest getIndexRequest = new GetIndexRequest("users");
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if(!exists){
            CreateIndexResponse indexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            System.out.println("response id: "+indexResponse.index());
        }
    }

    @PostMapping("/")
    public String save(@RequestBody User user) throws IOException {
        IndexRequest request = new IndexRequest("users");
        request.id(user.getUserId());
        request.source(new ObjectMapper().writeValueAsString(user), XContentType.JSON);
        IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
        System.out.println("response id: "+indexResponse.getId());
        return indexResponse.getResult().name();
    }

    @PostMapping("/async")
    public void indexAsync(@RequestBody User user) throws IOException {
        IndexRequest request = new IndexRequest("users");
        request.id(user.getUserId());
        request.source(new ObjectMapper().writeValueAsString(user), XContentType.JSON);
        client.indexAsync(request, RequestOptions.DEFAULT,listener);
        System.out.println("Request submitted !!!");
    }

    @GetMapping("/{id}")
    public User read(@PathVariable final String id) throws IOException {
        GetRequest getRequest = new GetRequest("users",id);
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        User user = new ObjectMapper().readValue(getResponse.getSourceAsString(),User.class);
        return user;
    }

    @GetMapping("/")
    public List<User> readAll() throws IOException {
        List<User> users = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest("users");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.size(5);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        for(SearchHit searchHit : searchResponse.getHits().getHits()){
            User user = new ObjectMapper().readValue(searchHit.getSourceAsString(),User.class);
            users.add(user);
        }
        return users;
    }

    @GetMapping("/name/{field}")
    public List<User> searchByName(@PathVariable final String field) throws IOException {
        List<User> users = new ArrayList<>();
        QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("name", field)
                .fuzziness(Fuzziness.AUTO)
                .prefixLength(2)
                .maxExpansions(10);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(matchQueryBuilder);
        sourceBuilder.from(0);
        sourceBuilder.size(5);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
        for(SearchHit searchHit : searchResponse.getHits().getHits()){
            User user = new ObjectMapper().readValue(searchHit.getSourceAsString(),User.class);
            users.add(user);
        }
        return users;
    }

    @RequestMapping(value = "/",method =RequestMethod.PUT)
    public String update(@RequestBody User user) throws IOException {
        UpdateRequest updateRequest = new UpdateRequest("users",user.getUserId());
        updateRequest.doc(new ObjectMapper().writeValueAsString(user), XContentType.JSON);
        UpdateResponse updateResponse = client.update(updateRequest,RequestOptions.DEFAULT);
        System.out.println(updateResponse.getGetResult());

        return updateResponse.status().name();
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE)
    public String delete(@PathVariable final String id) throws IOException {
        DeleteRequest request = new DeleteRequest("users",id);
        DeleteResponse deleteResponse = client.delete(request,RequestOptions.DEFAULT);
        return deleteResponse.getResult().name();
    }

    ActionListener listener = new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse indexResponse) {
            System.out.println(" Document updated successfully !!!");
        }

        @Override
        public void onFailure(Exception e) {
            System.out.print(" Document creation failed !!!"+ e.getMessage());
        }
    };



}
