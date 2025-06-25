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
        this.fileStorageLocation = Paths.get(storageDir, "originals").toAbsolutePath().normalize();
        this.previewStorageLocation = Paths.get(storageDir, "previews").toAbsolutePath().normalize();

        try {
            Files.createDirectories(fileStorageLocation);
            Files.createDirectories(previewStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize storage", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file");
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;

        try {
            // Ensure target directory exists
            if (!Files.exists(fileStorageLocation)) {
                Files.createDirectories(fileStorageLocation);
            }

            Path targetLocation = fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new RuntimeException("File not found: " + fileName);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + fileName, ex);
        }
    }

    public String storePreview(byte[] previewBytes, String originalFileName) throws IOException {
        String previewFileName = "preview_" + UUID.randomUUID() + "_" + originalFileName + ".png";
        Path targetLocation = previewStorageLocation.resolve(previewFileName);
        Files.write(targetLocation, previewBytes);
        return previewFileName;
    }

    public Path getPreviewStorageLocation() {
        return this.previewStorageLocation;
    }

}