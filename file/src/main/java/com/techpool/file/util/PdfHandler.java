package com.techpool.file.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techpool.file.BaseFileHandler;
import com.techpool.file.ThumbnailService;

public class PdfHandler extends BaseFileHandler {
    private static final Logger log = LoggerFactory.getLogger(PdfHandler.class);

    public PdfHandler(ThumbnailService thumbnailService) {
        super(thumbnailService);
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        log.info("Attempting to generate preview for: {}", file.getAbsolutePath());
        try {
            try (PDDocument document = PDDocument.load(file)) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage pdfImage = renderer.renderImageWithDPI(0, 100);
                BufferedImage enhancedPreview = createEnhancedPreview(pdfImage, file);
                return thumbnailService.convertToByteArray(enhancedPreview);
            }
        } catch (Exception e) {
            BufferedImage errorImage = new BufferedImage(600, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = errorImage.createGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, 600, 200);
            g.setColor(Color.WHITE);
            g.drawString("Preview Error: " + e.getMessage(), 20, 100);
            g.dispose();
            return thumbnailService.convertToByteArray(errorImage);
        }
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.equals("application/pdf");
    }
}