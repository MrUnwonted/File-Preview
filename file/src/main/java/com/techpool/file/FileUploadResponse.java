package com.techpool.file;

public class FileUploadResponse {
    private String fileName;
    
    public FileUploadResponse(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileName() {
        return fileName;
    }
}