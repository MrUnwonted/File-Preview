package com.techpool.file;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

@RestController
@RequestMapping("/api/files")
public class FileController {
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

    @GetMapping("/preview/{fileName}")
    public ResponseEntity<byte[]> getPreview(@PathVariable String fileName) {
        byte[] preview = previewService.generatePreview(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(preview);
    }

    @GetMapping("/thumbnail/{fileName}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String fileName) {
        byte[] thumbnail = thumbnailService.generateThumbnail(fileName, 200, 200);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(thumbnail);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}