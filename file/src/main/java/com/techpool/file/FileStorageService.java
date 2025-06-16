package com.techpool.file;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.tomcat.util.file.ConfigurationSource.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {
    private final Path fileStorageLocation;
    private final Path previewStorageLocation;

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
            throw new RuntimeException("Could not create storage directories", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        
        try {
            Path targetLocation = fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }
}