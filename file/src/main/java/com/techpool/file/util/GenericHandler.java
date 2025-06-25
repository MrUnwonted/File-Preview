package com.techpool.file.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.techpool.file.ThumbnailService;

public class GenericHandler implements FileTypeHandler {
    private final ThumbnailService thumbnailService;

    public GenericHandler(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @Override
    public boolean supports(String mimeType) {
        return true; // Fallback for all unsupported types
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Draw generic file preview
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 800, 800);
        
        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(250, 200, 300, 300, 20, 20);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        String fileType = PreviewUtils.getFileTypeDescription(file.getName());
        PreviewUtils.drawCenteredString(g, fileType, 400, 350);
        
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.setColor(Color.BLACK);
        PreviewUtils.drawCenteredString(g, file.getName(), 400, 600);

        g.dispose();
        return thumbnailService.convertToByteArray(image);
    }
}