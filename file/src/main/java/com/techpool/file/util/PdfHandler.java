package com.techpool.file.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.techpool.file.BaseFileHandler;
import com.techpool.file.ThumbnailService;

public class PdfHandler extends BaseFileHandler {
    private static final Logger log = LoggerFactory.getLogger(PdfHandler.class);
    private static final float PDF_DPI = 150f;

    public PdfHandler(ThumbnailService thumbnailService) {
        super(thumbnailService);
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        log.info("Generating PDF preview for: {}", file.getAbsolutePath());

        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF contains no pages");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> pages = new ArrayList<BufferedImage>();

            // Render all pages
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                pages.add(renderer.renderImageWithDPI(i, PDF_DPI));
            }

            return generateMultiPagePreview(pages, file);
        } catch (Exception e) {
            log.error("PDF preview generation failed", e);
            return createErrorImage("PDF preview error: " + e.getMessage());
        }
    }

    private byte[] createErrorImage(String message) throws IOException {
        BufferedImage image = new BufferedImage(800, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 200);
        g.setColor(Color.RED);
        g.drawString(message, 20, 100);
        g.dispose();
        return thumbnailService.convertToByteArray(image);
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.equals("application/pdf");
    }
}