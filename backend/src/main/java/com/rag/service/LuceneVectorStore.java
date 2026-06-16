package com.rag.service;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class LuceneVectorStore {

    private final String indexBaseDir;
    private static final String FIELD_VECTOR = "vector";
    private static final String FIELD_TEXT = "chunk_text";
    private static final String FIELD_CHUNK_ID = "chunk_id";

    public LuceneVectorStore(@Value("${app.upload-dir}") String uploadDir) {
        this.indexBaseDir = Paths.get(System.getProperty("user.dir"), "vector-index").toString();
        // 确保目录存在
        try {
            java.nio.file.Files.createDirectories(Paths.get(indexBaseDir));
        } catch (IOException ignored) {}
    }

    /**
     * 索引文档的所有文本块向量
     */
    public void indexDocument(Long documentId, List<String> chunks, List<List<Float>> vectors) throws IOException {
        Path docIndexPath = Paths.get(indexBaseDir, "doc_" + documentId);
        // 删除旧索引
        deleteDocumentIndex(documentId);

        try (FSDirectory dir = FSDirectory.open(docIndexPath);
             IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig()
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE))) {

            for (int i = 0; i < chunks.size(); i++) {
                Document doc = new Document();
                doc.add(new StoredField(FIELD_CHUNK_ID, i));
                doc.add(new StoredField(FIELD_TEXT, chunks.get(i)));
                doc.add(new KnnFloatVectorField(FIELD_VECTOR, toArray(vectors.get(i))));
                writer.addDocument(doc);
            }
            writer.commit();
        }
    }

    /**
     * KNN 向量检索 — 返回最相似的 Top-K 文本块
     */
    public List<String> search(Long documentId, List<Float> queryVector, int topK) throws IOException {
        Path docIndexPath = Paths.get(indexBaseDir, "doc_" + documentId);
        if (!java.nio.file.Files.exists(docIndexPath)) {
            return Collections.emptyList();
        }

        List<String> results = new ArrayList<>();
        try (FSDirectory dir = FSDirectory.open(docIndexPath);
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            Query knnQuery = new KnnFloatVectorQuery(FIELD_VECTOR, toArray(queryVector), topK);
            TopDocs topDocs = searcher.search(knnQuery, topK);

            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(sd.doc);
                String text = doc.get(FIELD_TEXT);
                if (text != null) {
                    results.add(text);
                }
            }
        }
        return results;
    }

    /**
     * 删除指定文档的向量索引
     */
    public void deleteDocumentIndex(Long documentId) {
        Path docIndexPath = Paths.get(indexBaseDir, "doc_" + documentId);
        try {
            if (java.nio.file.Files.exists(docIndexPath)) {
                deleteRecursively(docIndexPath);
            }
        } catch (IOException ignored) {}
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists(Long documentId) {
        return java.nio.file.Files.exists(Paths.get(indexBaseDir, "doc_" + documentId));
    }

    private float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            try (var entries = java.nio.file.Files.list(path)) {
                entries.forEach(p -> {
                    try { deleteRecursively(p); } catch (IOException ignored) {}
                });
            }
        }
        java.nio.file.Files.deleteIfExists(path);
    }
}
