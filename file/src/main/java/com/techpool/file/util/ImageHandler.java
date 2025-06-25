package com.techpool.file.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.techpool.file.BaseFileHandler;
import com.techpool.file.ThumbnailService;

public class ImageHandler extends BaseFileHandler {
    
    public ImageHandler(ThumbnailService thumbnailService) {
        super(thumbnailService);
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        try {
            BufferedImage image = ImageIO.read(file);
            BufferedImage enhancedPreview = createEnhancedPreview(image, file);
            return thumbnailService.convertToByteArray(enhancedPreview);
        } catch (Exception e) {
            return createErrorPreview("Image preview error: " + e.getMessage());
        }
    }

    private byte[] createErrorPreview(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createErrorPreview'");
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && 
               (mimeType.startsWith("image/") || 
                mimeType.equals("application/octet-stream"));
    }
}