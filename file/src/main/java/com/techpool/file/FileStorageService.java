package com.techpool.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private Path fileStorageLocation;
    private Path previewStorageLocation;

    @Value("${file.storage-dir}")
    private String storageDir;

    @PostConstruct
    public void init() {
        try {
            // Initialize original files storage
            this.fileStorageLocation = Paths.get(storageDir, "originals").toAbsolutePath().normalize();
            Files.createDirectories(fileStorageLocation);

            // Initialize previews storage
            this.previewStorageLocation = Paths.get(storageDir, "previews").toAbsolutePath().normalize();
            Files.createDirectories(previewStorageLocation);

            log.info("File storage initialized at: {}", fileStorageLocation);
            log.info("Preview storage initialized at: {}", previewStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize storage directories", ex);
        }
    }

    public Path getPreviewStorageLocation() {
        return this.previewStorageLocation;
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }

        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;

        try {
            // Copy file to target location
            Path targetLocation = fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", targetLocation);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                log.error("File not found or not readable: {}", filePath);
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            log.error("File path is invalid: {}", fileName);
            throw new RuntimeException("File not found: " + fileName, ex);
        }
    }

    public String storePreview(byte[] previewBytes, String originalFileName) throws IOException {
        String previewFileName = "preview_" + UUID.randomUUID() + "_" + originalFileName + ".png";
        Path targetLocation = previewStorageLocation.resolve(previewFileName);
        Files.write(targetLocation, previewBytes);
        return previewFileName;
    }

}