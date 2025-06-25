package com.techpool.file.util;

import java.io.File;
import java.io.IOException;

public interface FileTypeHandler {
    boolean supports(String mimeType);
    byte[] generatePreview(File file) throws IOException;
}