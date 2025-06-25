package com.techpool.file;

import com.techpool.file.util.FileTypeHandler;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseFileHandler implements FileTypeHandler {
    protected static final int QR_WIDTH = 150;
    protected static final int MARGIN = 20;
    protected static final Color INFO_BG_COLOR = new Color(240, 240, 240);

    protected final ThumbnailService thumbnailService;

    public BaseFileHandler(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    protected BufferedImage createEnhancedPreview(BufferedImage mainImage, File file) {
        // Common QR code and info sidebar implementation
        int originalWidth = mainImage != null ? mainImage.getWidth() : 600;
        int originalHeight = mainImage != null ? mainImage.getHeight() : 800;

        int sidebarWidth = QR_WIDTH + MARGIN * 2;
        int totalWidth = originalWidth + sidebarWidth;
        int totalHeight = Math.max(originalHeight, 800);

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

            // Draw main content if exists
            if (mainImage != null) {
                g.drawImage(mainImage, sidebarWidth, 0, null);
            } else {
                // drawFallbackContent(g, sidebarWidth, file);
            }

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

    protected void drawFallbackContent(Graphics2D g, int xOffset, File file) {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(xOffset, 0, 600, 800);
        g.setColor(Color.BLACK);
        g.drawString("No preview available for " + file.getName(), xOffset + 20, 100);
    }

    protected void drawFileInfo(Graphics2D g, File file, int x, int y) {
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
                y += 5;
            } else {
                g.drawString(line, x, y);
                y += 20;
            }
        }
    }

    protected String generateQrContent(File file) {
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

    protected String shortenFileName(String name, int maxLength) {
        if (name.length() <= maxLength)
            return name;
        return name.substring(0, maxLength / 2) + "..." +
                name.substring(name.length() - maxLength / 2);
    }
}