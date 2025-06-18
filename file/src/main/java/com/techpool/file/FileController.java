package com.techpool.file;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileStorageService fileStorageService;
    private final PreviewService previewService;
    private final ThumbnailService thumbnailService;

    public FileController(FileStorageService fileStorageService,
            PreviewService previewService,
            ThumbnailService thumbnailService) {
        this.fileStorageService = fileStorageService;
        this.previewService = previewService;
        this.thumbnailService = thumbnailService;
    }

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String storedFileName = fileStorageService.storeFile(file);
        return ResponseEntity.ok(new FileUploadResponse(storedFileName));
    }

    // @GetMapping("/health")
    // public ResponseEntity<String> healthCheck() {
    // boolean storageOk = Files.exists(fileStorageLocation) &&
    // Files.isWritable(fileStorageLocation);

    // if (!storageOk) {
    // return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
    // .body("File storage not available");
    // }
    // return ResponseEntity.ok("OK");
    // }

    @GetMapping("/preview/{fileName}")
    public ResponseEntity<byte[]> getPreview(@PathVariable String fileName) {
        try {
            // Check if preview exists first
            Path previewPath = fileStorageService.getPreviewStorageLocation().resolve("preview_" + fileName + ".png");
            if (Files.exists(previewPath)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(Files.readAllBytes(previewPath));
            }

            // Generate new preview
            byte[] preview;
            try {
                preview = previewService.generatePreview(fileName);
            } catch (Exception e) {
                log.error("Preview generation failed for file: {}", fileName, e);
                preview = previewService.generateErrorPreview("Preview unavailable");
            }

            // Store the preview
            fileStorageService.storePreview(preview, fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(preview);
        } catch (Exception e) {
            log.error("Failed to process preview request for file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/thumbnail/{fileName}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String fileName) {
        try {
            // First check if file exists
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] thumbnail = thumbnailService.generateThumbnail(fileName, 200, 200);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(thumbnail);
        } catch (Exception e) {
            log.error("Thumbnail generation failed for: {}", fileName, e);
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