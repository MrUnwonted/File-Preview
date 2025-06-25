package com.techpool.file.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.techpool.file.PreviewService;
import com.techpool.file.QrCodeUtil;
import com.techpool.file.ThumbnailService;

public class PdfHandler implements FileTypeHandler {
    private static final Logger log = LoggerFactory.getLogger(PdfHandler.class);
    private static final int QR_WIDTH = 150;
    private static final int MARGIN = 20;
    private static final Color INFO_BG_COLOR = new Color(240, 240, 240);

    private final ThumbnailService thumbnailService;

    public PdfHandler(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        log.info("Attempting to generate preview for: {}", file.getAbsolutePath());
        try {
            try (PDDocument document = PDDocument.load(file)) {
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage pdfImage = renderer.renderImageWithDPI(0, 100); // First page

                // Create enhanced preview with sidebar
                BufferedImage enhancedPreview = createEnhancedPreview(pdfImage, file);
                return thumbnailService.convertToByteArray(enhancedPreview);
            }
        } catch (Exception e) {
            // Create error image
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

    private BufferedImage createEnhancedPreview(BufferedImage pdfImage, File file) {
        int originalWidth = pdfImage.getWidth();
        int originalHeight = pdfImage.getHeight();

        // Calculate new dimensions
        int sidebarWidth = QR_WIDTH + MARGIN * 2;
        int totalWidth = originalWidth + sidebarWidth;
        int totalHeight = Math.max(originalHeight, 800); // Minimum height

        BufferedImage combined = new BufferedImage(
                totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = combined.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            // Draw white background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, totalWidth, totalHeight);

            // Draw sidebar background
            g.setColor(INFO_BG_COLOR);
            g.fillRect(0, 0, sidebarWidth, totalHeight);

            // Draw original PDF content (offset by sidebar)
            g.drawImage(pdfImage, sidebarWidth, 0, null);

            // Generate and draw QR code
            String qrContent = generateQrContent(file);
            BufferedImage qrCode = QrCodeUtil.generateQrCode(qrContent, QR_WIDTH);
            g.drawImage(qrCode, MARGIN, MARGIN, null);

            // Add file information
            drawFileInfo(g, file, MARGIN, QR_WIDTH + MARGIN * 2);

            return combined;
        } finally {
            g.dispose();
        }
    }

    private void drawFileInfo(Graphics2D g, File file, int x, int y) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");

        String[] infoLines = {
                "File Information:",
                "Name: " + shortenFileName(file.getName(), 20),
                "Size: " + (file.length() / 1024) + " KB",
                "Generated: " + sdf.format(new java.util.Date()),
                "",
                "Scan QR code for",
                "quick access"
        };

        for (String line : infoLines) {
            if (line.isEmpty()) {
                y += 5; // Extra space
            } else {
                g.drawString(line, x, y);
                y += 20;
            }
        }
    }

    private String shortenFileName(String name, int maxLength) {
        if (name.length() <= maxLength)
            return name;
        return name.substring(0, maxLength / 2) + "..." +
                name.substring(name.length() - maxLength / 2);
    }

    private String generateQrContent(File file) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "FileVault Document\n" +
                        "-----------------\n" +
                        "Filename: %s\n" +
                        "Timestamp: %s\n" +
                        "Size: %d KB",
                file.getName(),
                sdf.format(new Date()),
                file.length() / 1024);
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.equals("application/pdf");
    }
}