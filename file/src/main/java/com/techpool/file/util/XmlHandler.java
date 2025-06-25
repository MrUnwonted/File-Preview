package com.techpool.file.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.techpool.file.ThumbnailService;

public class XmlHandler implements FileTypeHandler {
    private final ThumbnailService thumbnailService;

    public XmlHandler(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType.equals("application/xml") || mimeType.equals("text/xml");
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Dark background for code
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, 800, 800);

        int y = 50;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 30) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Simple syntax highlighting
                if (line.startsWith("<")) {
                    g.setColor(new Color(86, 156, 214)); // Blue for tags
                } else {
                    g.setColor(Color.WHITE);
                }
                
                if (line.length() > 100) line = line.substring(0, 100) + "...";
                g.drawString(line, 50, y);
                y += 20;
                lineCount++;
            }
        }

        g.dispose();
        return thumbnailService.convertToByteArray(image);
    }
}