package com.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChromaService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Value("${chroma.collection-name}")
    private String collectionName;

    public ChromaService(RestTemplate restTemplate,
                         @Value("${chroma.host}") String host,
                         @Value("${chroma.port}") int port) {
        this.restTemplate = restTemplate;
        this.baseUrl = host + ":" + port;
    }

    public void createCollection() {
        String url = baseUrl + "/api/v1/collections";
        Map<String, Object> body = new HashMap<>();
        body.put("name", collectionName);
        body.put("metadata", Map.of("description", "RAG document chunks"));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            // collection may already exist, ignore
        }
    }

    public void addChunks(Long documentId, List<String> chunks, List<List<Float>> embeddings) {
        String url = baseUrl + "/api/v1/collections/" + collectionName + "/add";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> metadatas = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("document_id", documentId.toString());
            meta.put("chunk_index", i);
            meta.put("chunk_text", chunks.get(i));
            metadatas.add(meta);
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ids.add("doc_" + documentId + "_chunk_" + i);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("embeddings", embeddings);
        body.put("metadatas", metadatas);

        restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
    }

    public List<Map<String, Object>> query(List<Float> queryEmbedding, Long documentId, int topK) {
        String url = baseUrl + "/api/v1/collections/" + collectionName + "/query";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("query_embeddings", List.of(queryEmbedding));
        body.put("n_results", topK);

        // 只检索指定文档的块
        Map<String, Object> whereFilter = new HashMap<>();
        whereFilter.put("document_id", documentId.toString());
        body.put("where", whereFilter);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);

        List<Map<String, Object>> results = new ArrayList<>();
        if (response.getBody() != null) {
            Map<String, Object> ids = (Map<String, Object>) response.getBody().get("ids");
            Map<String, Object> metadatas = (Map<String, Object>) response.getBody().get("metadatas");
            Map<String, Object> distances = (Map<String, Object>) response.getBody().get("distances");

            if (ids != null) {
                // Flatten: Chroma returns nested lists for batch queries
                List<List<String>> idList = (List<List<String>>) ids.get("ids");
                List<List<Map<String, Object>>> metaList = (List<List<Map<String, Object>>>) metadatas.get("metadatas");
                List<List<Double>> distList = (List<List<Double>>) distances.get("distances");

                if (idList != null && !idList.isEmpty()) {
                    List<String> flatIds = idList.get(0);
                    List<Map<String, Object>> flatMetas = metaList != null ? metaList.get(0) : null;
                    List<Double> flatDists = distList != null ? distList.get(0) : null;

                    for (int i = 0; i < flatIds.size(); i++) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", flatIds.get(i));
                        if (flatMetas != null) {
                            result.put("metadata", flatMetas.get(i));
                        }
                        if (flatDists != null) {
                            result.put("distance", flatDists.get(i));
                        }
                        results.add(result);
                    }
                }
            }
        }
        return results;
    }
}
