package com.tinysteps.reportservice.controller;

import com.tinysteps.reportservice.entity.Report;
import com.tinysteps.reportservice.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/v1/reports/download")
@RequiredArgsConstructor
@Slf4j
public class ReportDownloadController {

    private final ReportRepository reportRepository;
    
    @Value("${report.storage.path}")
    private String reportStoragePath;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String filename) {
        log.info("Request to download report file: {}", filename);
        
        Path filePath = Paths.get(reportStoragePath).resolve(filename).normalize();
        Resource resource = new FileSystemResource(filePath.toFile());
        
        if (!resource.exists()) {
            log.error("File not found: {}", filename);
            return ResponseEntity.notFound().build();
        }
        
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = determineContentType(filename);
            }
        } catch (IOException e) {
            log.warn("Could not determine file type for {}", filename);
            contentType = determineContentType(filename);
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    private String determineContentType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.toLowerCase().endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            return "application/octet-stream";
        }
    }
}