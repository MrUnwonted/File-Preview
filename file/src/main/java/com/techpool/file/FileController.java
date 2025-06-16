package com.techpool.file;

import java.net.http.HttpHeaders;

import org.apache.tomcat.util.http.parser.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;
    private final PreviewService previewService;

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
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}