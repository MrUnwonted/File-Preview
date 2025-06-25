package com.techpool.file.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.techpool.file.ThumbnailService;

public class ImageHandler implements FileTypeHandler {
    private final ThumbnailService thumbnailService;

    public ImageHandler(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType.startsWith("image/");
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        BufferedImage resizedImage = thumbnailService.resizeImage(image, 800, 800);
        BufferedImage finalImage = PreviewUtils.addQrCodeToPreview(resizedImage, file);
        return thumbnailService.convertToByteArray(finalImage);
    }
}