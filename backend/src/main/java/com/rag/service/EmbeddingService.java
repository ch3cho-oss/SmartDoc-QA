package com.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 文本向量化服务
 * - simple: 本地 TF-IDF + 字符 n-gram 混合向量，512维，无需外部 API
 * - zhipu: 智谱 Embedding API
 * - deepseek: DeepSeek Embedding API
 */
@Service
public class EmbeddingService {

    private static final int VECTOR_DIM = 512;

    private final RestTemplate restTemplate;

    @Value("${embedding.provider}")
    private String provider;

    @Value("${embedding.api-key}")
    private String apiKey;

    @Value("${embedding.model}")
    private String model;

    public EmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Float> embed(String text) {
        return switch (provider) {
            case "simple" -> embedSimple(text);
            case "zhipu" -> embedZhipu(text);
            case "deepseek" -> embedDeepSeek(text);
            default -> embedSimple(text);
        };
    }

    /**
     * 批量向量化 — 用于文档处理时对整个语料计算 TF-IDF
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        if ("simple".equals(provider)) {
            return embedBatchSimple(texts);
        }
        List<List<Float>> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    /**
     * TF-IDF 加权向量化
     * 1. 对整个语料建词表，计算每个词的 IDF
     * 2. 对每个文本，用 TF-IDF 权重填充向量
     * 3. L2 归一化
     *
     * 相比纯 n-gram 哈希，TF-IDF 能区分重要词和常见词，检索精度显著提升
     */
    private List<List<Float>> embedBatchSimple(List<String> texts) {
        int N = texts.size();

        // 第一步：统计每个 n-gram 在多少个文档中出现（DF）
        Map<String, Integer> docFreq = new HashMap<>();
        List<Map<String, Integer>> termFreqs = new ArrayList<>();

        for (String text : texts) {
            String normalized = text.toLowerCase().replaceAll("\\s+", "");
            Map<String, Integer> tf = new HashMap<>();
            for (int n = 1; n <= 3; n++) {
                for (int i = 0; i <= normalized.length() - n; i++) {
                    String ngram = normalized.substring(i, i + n);
                    tf.merge(ngram, 1, Integer::sum);
                }
            }
            termFreqs.add(tf);
            // 每个 n-gram 在每个文档中只计一次 DF
            for (String ngram : tf.keySet()) {
                docFreq.merge(ngram, 1, Integer::sum);
            }
        }

        // 第二步：为每个文本计算 TF-IDF 向量
        List<List<Float>> result = new ArrayList<>();
        for (Map<String, Integer> tf : termFreqs) {
            float[] vec = new float[VECTOR_DIM];
            double totalWeight = 0;

            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                String ngram = entry.getKey();
                int freq = entry.getValue();
                int df = docFreq.getOrDefault(ngram, 1);

                // TF-IDF
                double idf = Math.log((double) (N + 1) / (df + 1)) + 1.0;
                double tfidf = Math.log(1 + freq) * idf;

                // 对 n-gram 长度加权：越长的 n-gram 越有区分度
                double ngramBoost = 1.0 + (ngram.length() - 1) * 0.5;
                double weight = tfidf * ngramBoost;

                // 双哈希映射到向量位置
                int[] indices = hashToIndices(ngram);
                vec[indices[0]] += (float) weight;
                vec[indices[1]] += (float) (weight * 0.5);
                totalWeight += weight * weight;
            }

            // L2 归一化
            float norm = (float) Math.sqrt(totalWeight);
            if (norm > 0) {
                for (int i = 0; i < VECTOR_DIM; i++) {
                    vec[i] /= norm;
                }
            }

            List<Float> vecList = new ArrayList<>(VECTOR_DIM);
            for (float v : vec) vecList.add(v);
            result.add(vecList);
        }

        return result;
    }

    /**
     * 单文本向量化 — 使用 TF-IDF 近似（无语料时退化为 n-gram 哈希）
     */
    private List<Float> embedSimple(String text) {
        float[] vec = new float[VECTOR_DIM];
        String normalized = text.toLowerCase().replaceAll("\\s+", "");

        try {
            Map<String, Integer> tf = new HashMap<>();
            for (int n = 1; n <= 3; n++) {
                for (int i = 0; i <= normalized.length() - n; i++) {
                    String ngram = normalized.substring(i, i + n);
                    tf.merge(ngram, 1, Integer::sum);
                }
            }

            double totalWeight = 0;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                String ngram = entry.getKey();
                int freq = entry.getValue();

                double tfWeight = Math.log(1 + freq);
                double ngramBoost = 1.0 + (ngram.length() - 1) * 0.5;
                double weight = tfWeight * ngramBoost;

                int[] indices = hashToIndices(ngram);
                vec[indices[0]] += (float) weight;
                vec[indices[1]] += (float) (weight * 0.5);
                totalWeight += weight * weight;
            }

            float norm = (float) Math.sqrt(totalWeight);
            if (norm > 0) {
                for (int i = 0; i < VECTOR_DIM; i++) vec[i] /= norm;
            }
        } catch (Exception e) {
            for (int i = 0; i < VECTOR_DIM; i++) vec[i] = (float) Math.random();
        }

        List<Float> result = new ArrayList<>(VECTOR_DIM);
        for (float v : vec) result.add(v);
        return result;
    }

    /**
     * 将字符串哈希到 2 个向量索引位置
     */
    private int[] hashToIndices(String s) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hash = md5.digest(s.getBytes(StandardCharsets.UTF_8));
            int idx1 = ((hash[0] & 0xFF) << 8 | (hash[1] & 0xFF)) % VECTOR_DIM;
            int idx2 = ((hash[2] & 0xFF) << 8 | (hash[3] & 0xFF)) % VECTOR_DIM;
            return new int[]{idx1, idx2};
        } catch (Exception e) {
            return new int[]{(int) (Math.random() * VECTOR_DIM), (int) (Math.random() * VECTOR_DIM)};
        }
    }

    public static double cosineSimilarity(List<Float> a, List<Float> b) {
        double dot = 0, normA = 0, normB = 0;
        int dim = Math.min(a.size(), b.size());
        for (int i = 0; i < dim; i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Float> embedZhipu(String text) {
        String url = "https://open.bigmodel.cn/api/paas/v4/embeddings";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            if (data != null && !data.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Float> embedding = (List<Float>) data.get(0).get("embedding");
                return embedding;
            }
        }
        throw new RuntimeException("智谱 Embedding API 调用失败");
    }

    private List<Float> embedDeepSeek(String text) {
        String url = "https://api.deepseek.com/v1/embeddings";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", text);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            if (data != null && !data.isEmpty()) {
                @SuppressWarnings("unchecked")
                List<Float> embedding = (List<Float>) data.get(0).get("embedding");
                return embedding;
            }
        }
        throw new RuntimeException("DeepSeek Embedding API 调用失败");
    }
}
