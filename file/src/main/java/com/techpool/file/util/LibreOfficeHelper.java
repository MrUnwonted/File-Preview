package com.techpool.file.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;

public class LibreOfficeHelper {
    public static List<BufferedImage> convertToImages(File inputFile, String libreOfficePath) throws Exception {
        Path tempDir = Files.createTempDirectory("lo-preview-");
        try {
            Process process = new ProcessBuilder(
                    libreOfficePath,
                    "--headless",
                    "--convert-to", "png",
                    "--outdir", tempDir.toString(),
                    inputFile.getAbsolutePath())
                .start();
            
            if (!process.waitFor(2, TimeUnit.MINUTES)) {
                throw new IOException("Conversion timeout");
            }
            
            List<BufferedImage> pages = new ArrayList<>();
            try (Stream<Path> files = Files.list(tempDir)) {
                files.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                     .sorted()
                     .forEach(p -> {
                         try {
                             pages.add(ImageIO.read(p.toFile()));
                         } catch (IOException e) {
                             throw new RuntimeException(e);
                         }
                     });
            }
            return pages;
        } finally {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }
}