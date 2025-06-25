package com.techpool.file;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class QrCodeUtil {
    public static BufferedImage generateQrCode(String text, int size) {
        try {
            Map<EncodeHintType, Object> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hintMap.put(EncodeHintType.MARGIN, 1);
            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix byteMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hintMap);

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, size, size);
                graphics.setColor(Color.BLACK);

                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (byteMatrix.get(i, j)) {
                            graphics.fillRect(i, j, 1, 1);
                        }
                    }
                }
                return image;
            } finally {
                graphics.dispose();
            }
        } catch (WriterException e) {
            // Fallback: create error image
            BufferedImage errorImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = errorImage.createGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.drawString("QR Error", 10, size/2);
            g.dispose();
            return errorImage;
        }
    }
}