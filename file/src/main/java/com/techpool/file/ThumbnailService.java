package com.techpool.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ThumbnailService {
    private final FileStorageService fileStorageService;

    // Add constructor to initialize fileStorageService
    public ThumbnailService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public byte[] generateThumbnail(String storedFileName, int width, int height) {
        try {
            Resource fileResource = fileStorageService.loadFileAsResource(storedFileName);
            File file = fileResource.getFile();
            BufferedImage originalImage = ImageIO.read(file);
            BufferedImage thumbnail = resizeImage(originalImage, width, height);
            return convertToByteArray(thumbnail);
        } catch (Exception e) {
            throw new RuntimeException("Thumbnail generation failed", e);
        }
    }

    public BufferedImage resizeImage(BufferedImage image, int maxWidth, int maxHeight) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // Calculate new dimensions while maintaining aspect ratio
        double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    public byte[] convertToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}