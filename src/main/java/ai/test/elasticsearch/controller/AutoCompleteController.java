package ai.test.elasticsearch.controller;

import ai.test.elasticsearch.entity.TextSearch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by suman.das on 7/1/19.
 */
@RestController()
@RequestMapping("/autocomplete")
public class AutoCompleteController {

    @Autowired
    private RestHighLevelClient client;

    @PostMapping("/index")
    public void index() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("auto_complete_index");
        request.settings("{\"analysis\":{\"analyzer\":{\"autocomplete\":{\"tokenizer\":\"autocomplete\",\"filter\":[\"lowercase\"]},\"autocomplete_search\":{\"tokenizer\":\"lowercase\"}},\"tokenizer\":{\"autocomplete\":{\"type\":\"edge_ngram\",\"min_gram\":2,\"max_gram\":10,\"token_chars\":[\"letter\",\"digit\"]}}}}", XContentType.JSON);
        request.mapping("{\"properties\":{\"title\":{\"type\":\"text\",\"analyzer\":\"autocomplete\",\"search_analyzer\":\"autocomplete_search\",\"suggest\":{\"type\":\"completion\"}}}}",XContentType.JSON);
        GetIndexRequest getIndexRequest = new GetIndexRequest("auto_complete_index");
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if(!exists){
            CreateIndexResponse indexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            System.out.println("response id: "+indexResponse.index());
        }
    }

    @RequestMapping(value = "/",method =RequestMethod.PUT)
    public String update(@RequestBody TextSearch textSearch) throws IOException {
        IndexRequest request = new IndexRequest("auto_complete_index");
        request.source(new ObjectMapper().writeValueAsString(textSearch), XContentType.JSON);
        IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
        System.out.println("response id: "+indexResponse.getId());
        return indexResponse.getResult().name();
    }

    @GetMapping("/{field}/")
    public List<TextSearch> searchByName(@PathVariable final String field) throws IOException {
        List<TextSearch> users = new ArrayList<>();
        SearchRequest searchRequest = new SearchRequest("auto_complete_index");
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("title", field);
        matchQueryBuilder.operator(Operator.OR);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(matchQueryBuilder);
        sourceBuilder.from(0);
        sourceBuilder.size(5);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest,RequestOptions.DEFAULT);
        for(SearchHit searchHit : searchResponse.getHits().getHits()){
            TextSearch user = new ObjectMapper().readValue(searchHit.getSourceAsString(),TextSearch.class);
            users.add(user);
        }
        return users;


    }



}
