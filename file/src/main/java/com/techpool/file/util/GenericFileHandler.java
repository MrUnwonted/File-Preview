package com.techpool.file.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.techpool.file.BaseFileHandler;
import com.techpool.file.ThumbnailService;

public class GenericFileHandler extends BaseFileHandler {
    
    public GenericFileHandler(ThumbnailService thumbnailService) {
        super(thumbnailService);
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        BufferedImage enhancedPreview = createEnhancedPreview(null, file);
        return thumbnailService.convertToByteArray(enhancedPreview);
    }

    @Override
    public boolean supports(String mimeType) {
        return true; // Catches all remaining file types
    }
}