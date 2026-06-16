package com.rag.service;

import com.rag.entity.Document;
import com.rag.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.chunk-size}")
    private int chunkSize;

    @Value("${app.chunk-overlap}")
    private int chunkOverlap;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document uploadAndParse(Long userId, MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String ext = getExtension(fileName).toLowerCase();

        if (!ext.equals("txt") && !ext.equals("pdf")) {
            throw new RuntimeException("仅支持 .txt 和 .pdf 格式的文件");
        }

        // 保存文件 — 使用绝对路径避免 Tomcat 临时目录问题
        Path basePath = Paths.get(uploadDir);
        if (!basePath.isAbsolute()) {
            basePath = Paths.get(System.getProperty("user.dir")).resolve(uploadDir);
        }
        Path userDir = basePath.resolve(userId.toString());
        Files.createDirectories(userDir);
        String savedName = System.currentTimeMillis() + "_" + fileName;
        Path filePath = userDir.resolve(savedName);
        file.transferTo(filePath.toFile());

        // 创建文档记录
        Document document = Document.builder()
                .userId(userId)
                .fileName(fileName)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .status(Document.DocStatus.PROCESSING)
                .build();
        document = documentRepository.save(document);

        return document;
    }

    public String extractText(Document document) throws Exception {
        Path filePath = Paths.get(document.getFilePath());
        String ext = getExtension(document.getFileName()).toLowerCase();

        if ("txt".equals(ext)) {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } else if ("pdf".equals(ext)) {
            try (var pdfDoc = Loader.loadPDF(filePath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setLineSeparator("\n");
                stripper.setSortByPosition(true);
                return stripper.getText(pdfDoc);
            }
        }
        throw new RuntimeException("不支持的文件格式: " + ext);
    }

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            if (end >= text.length()) break;
            start = end - chunkOverlap;
        }
        return chunks;
    }

    public void updateStatus(Long docId, Document.DocStatus status, Integer totalChunks) {
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setStatus(status);
            if (totalChunks != null) {
                doc.setTotalChunks(totalChunks);
            }
            documentRepository.save(doc);
        });
    }

    public List<Document> getUserDocuments(Long userId) {
        return documentRepository.findByUserIdOrderByUploadTimeDesc(userId);
    }

    public Document getDocument(Long docId) {
        return documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
    }

    public void deleteDocument(Long docId) {
        documentRepository.deleteById(docId);
    }

    public void updateDocument(Document doc) {
        documentRepository.save(doc);
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return idx == -1 ? "" : fileName.substring(idx + 1);
    }
}
