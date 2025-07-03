package com.techpool.file;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/files")
@CrossOrigin("http://localhost:4200")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileStorageService fileStorageService;
    private final PreviewService previewService;

    public FileController(FileStorageService fileStorageService,
            PreviewService previewService,
            ThumbnailService thumbnailService) {
        this.fileStorageService = fileStorageService;
        this.previewService = previewService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String storedFileName = fileStorageService.storeFile(file);
        return ResponseEntity.ok(new FileUploadResponse(storedFileName));
    }

    @GetMapping("/preview/{fileName}")
    public ResponseEntity<byte[]> getPreview(@PathVariable String fileName) {
        log.info("Generating preview for: {}", fileName); // Add this
        try {
            // Generate new preview if needed
            byte[] preview = previewService.generatePreview(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(preview);
        } catch (Exception e) {
            log.error("Failed to process preview request for file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/multipage-preview/{fileName}")
    public ResponseEntity<byte[]> getMultiPagePreview(@PathVariable String fileName) {
        try {
            byte[] preview = previewService.generateMultiPagePreview(fileName);
            preview = previewService.validateImage(preview); // Add validation

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header("X-Preview-Validated", "true")
                    .body(preview);
        } catch (Exception e) {
            log.error("Multi-page preview failed for: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Preview generation failed: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/page-count/{fileName}")
    public ResponseEntity<Integer> getPdfPageCount(@PathVariable String fileName) {
        try {
            Resource fileResource = fileStorageService.loadFileAsResource(fileName);
            File file = fileResource.getFile();

            if (file.getName().toLowerCase().endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file)) {
                    return ResponseEntity.ok(document.getNumberOfPages());
                }
            }
            return ResponseEntity.ok(1); // Non-PDF files are considered single-page
        } catch (Exception e) {
            log.error("Failed to get page count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}