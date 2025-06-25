package com.techpool.file.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import com.techpool.file.QrCodeUtil;

public class PreviewUtils {
    public static BufferedImage addQrCodeToPreview(BufferedImage originalImage, File file) {
        try {
            String qrContent = "File: " + file.getName() + "\n" +
                    "Size: " + (file.length() / 1024) + " KB\n" +
                    "Preview generated on: " + new java.util.Date();

            BufferedImage qrCode = QrCodeUtil.generateQrCode(qrContent, 150);

            BufferedImage combined = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight() + qrCode.getHeight() + 20,
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g = combined.createGraphics();
            g.drawImage(originalImage, 0, 0, null);
            
            int qrX = (originalImage.getWidth() - qrCode.getWidth()) / 2;
            int qrY = originalImage.getHeight() + 10;
            g.drawImage(qrCode, qrX, qrY, null);
            
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            String qrText = "Scan for file information";
            int textWidth = g.getFontMetrics().stringWidth(qrText);
            g.drawString(qrText, (originalImage.getWidth() - textWidth)/2, qrY + qrCode.getHeight() + 15);
            
            g.dispose();
            return combined;
        } catch (Exception e) {
            return originalImage;
        }
    }

   public static void drawCenteredString(Graphics2D g, String text, int x, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x - textWidth / 2, y);
    }

    public static String getFileTypeDescription(String filename) {
        if (filename.endsWith(".txt")) return "Text File";
        if (filename.endsWith(".zip") || filename.endsWith(".rar")) return "Archive";
        if (filename.endsWith(".mp3") || filename.endsWith(".wav")) return "Audio";
        if (filename.endsWith(".mp4") || filename.endsWith(".avi")) return "Video";
        return "File";
    }
}