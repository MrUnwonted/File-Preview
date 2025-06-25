package com.techpool.file.util;

import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.techpool.file.ThumbnailService;

public class ExcelHandler implements FileTypeHandler {
    private static final Logger log = LoggerFactory.getLogger(ExcelHandler.class);
    private final ThumbnailService thumbnailService;

    public ExcelHandler(ThumbnailService thumbnailService,
            String libreOfficePath,
            long libreOfficeTimeout) {
        this.thumbnailService = thumbnailService;
    }

    @Override
    public boolean supports(String mimeType) {
        return mimeType.contains("excel") || mimeType.contains("spreadsheetml");
    }

    @Override
    public byte[] generatePreview(File file) throws IOException {
        try {
            return generateWithLibreOffice(file);
        } catch (Exception e) {
            return generateWithPoi(file);
        }
    }

    public byte[] generateWithLibreOffice(File file) throws Exception {
        Path tempDir = Files.createTempDirectory("lo-preview-");
        try {
            String sofficePath = getLibreOfficePath();
            log.info("Attempting conversion with LibreOffice at: {}", sofficePath);

            ProcessBuilder pb = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--convert-to", "png",
                    "--outdir", tempDir.toString(),
                    file.getAbsolutePath());

            // Redirect all output to log file
            File logFile = tempDir.resolve("conversion.log").toFile();
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            Process process = pb.start();
            boolean success = process.waitFor(2, TimeUnit.MINUTES);

            // Read the log file regardless of success
            String logContent = Files.readString(logFile.toPath());
            log.info("LibreOffice conversion log:\n{}", logContent);

            if (!success) {
                process.destroyForcibly();
                throw new IOException("Conversion timed out after 2 minutes");
            }

            if (process.exitValue() != 0) {
                throw new IOException("LibreOffice failed with exit code " +
                        process.exitValue() + "\nLogs:\n" + logContent);
            }

            // Find the generated PNG
            try (Stream<Path> files = Files.list(tempDir)) {
                Optional<Path> pngFile = files
                        .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                        .findFirst();

                if (pngFile.isEmpty()) {
                    throw new IOException("No PNG file generated. Logs:\n" + logContent);
                }

                BufferedImage image = ImageIO.read(pngFile.get().toFile());
                if (image == null) {
                    throw new IOException("Generated PNG is invalid");
                }

                return thumbnailService.convertToByteArray(
                        thumbnailService.resizeImage(image, 800, 800));
            }
        } catch (Exception e) {
            log.error("LibreOffice conversion failed", e);
            throw e;
        } finally {
            try {
                FileUtils.deleteDirectory(tempDir.toFile());
            } catch (IOException e) {
                log.warn("Failed to clean temp directory: {}", tempDir, e);
            }
        }
    }

    // // Helper: Detect LibreOffice path based on OS
    private String getLibreOfficePath() throws IOException {
        String customPath = "C:\\LibreOfficePortable\\App\\LibreOffice\\program\\soffice.exe";
        File loFile = new File(customPath);

        if (!loFile.exists()) {
            throw new IOException("LibreOffice not found at: " + customPath +
                    "\nPlease verify installation path or install LibreOffice Portable");
        }
        return customPath;
    }

    private byte[] generateWithPoi(File file) throws IOException {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 800);

        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Excel Preview", 50, 50);

        try (Workbook workbook = WorkbookFactory.create(file)) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 16));

            int y = 100;
            for (int i = 0; i < workbook.getNumberOfSheets() && y < 700; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                g.drawString("Sheet " + (i + 1) + ": " + sheet.getSheetName(), 50, y);
                y += 20;
            }
        }

        g.dispose();
        return thumbnailService.convertToByteArray(image);
    }
}