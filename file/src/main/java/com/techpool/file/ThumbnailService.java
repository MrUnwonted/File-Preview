package com.techpool.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class ThumbnailService {
    public byte[] generateThumbnail(String storedFileName, int width, int height) {
        try {
            byte[] preview = previewService.generatePreview(storedFileName);
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(preview));
            BufferedImage thumbnail = ThumbnailService.of(originalImage)
                    .size(width, height)
                    .asBufferedImage();
            return convertToByteArray(thumbnail);
        } catch (Exception e) {
            throw new RuntimeException("Thumbnail generation failed", e);
        }
    }

    public byte[] resizeImage(BufferedImage image, int maxWidth, int maxHeight) throws IOException {
        return ThumbnailService.of(image)
                .size(maxWidth, maxHeight)
                .asBufferedImage();
    }

    public byte[] convertToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}