package com.techpool.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class PreviewService {
    private final FileStorageService fileStorageService;
    private final ThumbnailService thumbnailService;

    public byte[] generatePreview(String storedFileName) {
        try {
            Resource fileResource = fileStorageService.loadFileAsResource(storedFileName);
            File file = fileResource.getFile();
            
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = new Tika().detect(file);
            }

            if (mimeType.startsWith("image/")) {
                return generateImagePreview(file);
            } else if (mimeType.equals("application/pdf")) {
                return generatePdfPreview(file);
            } else if (mimeType.contains("word") || mimeType.contains("excel")) {
                return generateOfficePreview(file);
            } else {
                return generateGenericPreview(file);
            }
        } catch (Exception e) {
            throw new RuntimeException("Preview generation failed", e);
        }
    }

    private byte[] generateImagePreview(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        return thumbnailService.resizeImage(image, 800, 800);
    }

    private byte[] generatePdfPreview(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 100);
            return thumbnailService.resizeImage(image, 800, 800);
        }
    }

    private byte[] generateOfficePreview(File file) {
        // Implementation for Word/Excel using Apache POI
        // Return a generic preview if specific implementation isn't available
        return generateGenericPreview();
    }

    private byte[] generateGenericPreview() {
        // Generate a generic file icon with file type info
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        // Draw generic preview
        graphics.dispose();
        return thumbnailService.convertToByteArray(image);
    }
}