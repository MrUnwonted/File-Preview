package com.techpool.file.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.techpool.file.ThumbnailService;

@Component
public class FileTypeHandlerFactory {
    private final ThumbnailService thumbnailService;
    private final String libreOfficePath;
    private final long libreOfficeTimeout;
    private List<FileTypeHandler> handlers;

    public FileTypeHandlerFactory(ThumbnailService thumbnailService,
                                @Value("${libreoffice.path}") String libreOfficePath,
                                @Value("${libreoffice.timeout:120000}") long libreOfficeTimeout) {
        this.thumbnailService = thumbnailService;
        this.libreOfficePath = libreOfficePath;
        this.libreOfficeTimeout = libreOfficeTimeout;
        initializeHandlers();
    }

    private void initializeHandlers() {
        this.handlers = List.of(
            new ImageHandler(thumbnailService),
            new PdfHandler(thumbnailService),
            new WordHandler(thumbnailService, libreOfficePath),
            new ExcelHandler(thumbnailService, libreOfficePath, libreOfficeTimeout),
            new CsvHandler(thumbnailService),
            new XmlHandler(thumbnailService),
            new GenericHandler(thumbnailService)
        );
    }

    public FileTypeHandler getHandler(String mimeType) {
        return handlers.stream()
                .filter(handler -> handler.supports(mimeType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler found for mimeType: " + mimeType));
    }
}