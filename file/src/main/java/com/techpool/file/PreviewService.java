package com.techpool.file;

// import org.docx4j.convert.out.pdf.PdfSettings;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.commons.io.FileUtils;
import java.nio.file.*;

@Service
public class PreviewService {
    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private final FileStorageService fileStorageService;
    private final ThumbnailService thumbnailService;
    @Value("${libreoffice.path}")
    private String libreOfficePath;
    @Value("${libreoffice.timeout:120000}") // Default 2 minutes
    private long libreOfficeTimeout; // Changed to primitive long

    public PreviewService(FileStorageService fileStorageService, ThumbnailService thumbnailService) {
        this.fileStorageService = fileStorageService;
        this.thumbnailService = thumbnailService;
    }

    public byte[] generatePreview(String storedFileName) {
        try {
            Resource fileResource = fileStorageService.loadFileAsResource(storedFileName);
            File file = fileResource.getFile();
            String mimeType = detectMimeType(file);

            if (mimeType == null) {
                return generateGenericPreview("Unknown File Type", file.getName());
            }

            if (mimeType.startsWith("image/")) {
                return generateImagePreview(file);
            } else if (mimeType.equals("application/pdf")) {
                return generatePdfPreview(file);
            } else if (mimeType.contains("word") || mimeType.contains("officedocument.wordprocessingml")) {
                return generateWordPreview(file);
            } else {
                return generateGenericPreview(file, mimeType);
            }
        } catch (Exception e) {
            log.error("Preview generation failed for: " + storedFileName, e);
            try {
                return generateErrorPreview("Preview generation failed");
            } catch (IOException ioException) {
                throw new RuntimeException("Failed to generate error preview", ioException);
            }
        }
    }

    public byte[] generateErrorPreview(String message) throws IOException {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 800);

        // Error message
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Preview Error", 50, 50);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString(message, 50, 100);

        g.dispose();
        return thumbnailService.convertToByteArray(image);
    }

    private byte[] generateImagePreview(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        BufferedImage resizedImage = thumbnailService.resizeImage(image, 800, 800);
        return thumbnailService.convertToByteArray(resizedImage);
    }

    private byte[] generatePdfPreview(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 100);
            BufferedImage resizedImage = thumbnailService.resizeImage(image, 800, 800);
            return thumbnailService.convertToByteArray(resizedImage);
        }
    }

    private byte[] generateWordPreview(File file) {
        try {
            // First try with docx4j
            // return generateWordPreviewWithDocx4j(file);
            return generateWordPreviewWithLibreOffice(file);
        } catch (Exception e) {
            // log.warn("Docx4j conversion failed, falling back to text preview", e);
            log.warn("LibreOffice conversion failed, falling back to text preview", e);
            try {
                return generateWordPreviewWithPoi(file);
            } catch (Exception ex) {
                // log.error("Both docx4j and POI failed", ex);
                log.error("Both LibreOffice and POI failed", ex);
                try {
                    return generateErrorPreview("Preview unavailable");
                } catch (IOException ioex) {
                    throw new RuntimeException("Failed to generate error preview", ioex);
                }
            }
        }
    }

    public byte[] generateWordPreviewWithLibreOffice(File file) throws Exception {
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

    // Enhanced image processing
    private BufferedImage enhanceImageQuality(BufferedImage original) {
        BufferedImage enhanced = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = enhanced.createGraphics();
        try {
            // High-quality rendering
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Contrast adjustment
            RescaleOp rescaleOp = new RescaleOp(1.2f, 15, null);
            g.drawImage(original, rescaleOp, 0, 0);
        } finally {
            g.dispose();
        }
        return enhanced;
    }

    // Helper: Detect LibreOffice path based on OS
    private String getLibreOfficePath() throws IOException {
        String customPath = "C:\\LibreOfficePortable\\App\\LibreOffice\\program\\soffice.exe";
        File loFile = new File(customPath);

        if (!loFile.exists()) {
            throw new IOException("LibreOffice not found at: " + customPath +
                    "\nPlease verify installation path or install LibreOffice Portable");
        }
        return customPath;
    }

    private byte[] generateWordPreviewWithPoi(File file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            BufferedImage image = new BufferedImage(1200, 1600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // White background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 1200, 1600);

            // Draw title
            g.setColor(new Color(0, 0, 128));
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("DOCUMENT PREVIEW", 50, 50);

            int y = 100;

            // Process all pictures in document first
            for (XWPFPictureData picture : doc.getAllPictures()) {
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(picture.getData()));
                    if (img != null) {
                        double scaleFactor = 1000.0 / img.getWidth();
                        int newHeight = (int) (img.getHeight() * scaleFactor);
                        g.drawImage(img, 100, y, 1000, newHeight, null);
                        y += newHeight + 20;
                    }
                } catch (Exception e) {
                    log.warn("Failed to process image", e);
                }
            }

            // Process paragraphs
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = extractFormattedText(p);
                if (!text.trim().isEmpty()) {
                    y = drawFormattedText(g, text, 100, y, 1000);
                    y += 20; // Add space between paragraphs
                }

                if (y > 1500)
                    break;
            }

            g.dispose();

            // Crop unused space
            BufferedImage cropped = image.getSubimage(0, 0, 1200, Math.min(y + 50, 1600));
            return thumbnailService.convertToByteArray(cropped);
        }
    }

    private String detectMimeType(File file) throws IOException {
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            Tika tika = new Tika();
            mimeType = tika.detect(file);
        }
        return mimeType;
    }

    private String extractFormattedText(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : p.getRuns()) {
            String text = run.text();
            if (text != null && !text.trim().isEmpty()) {
                if (run.isBold()) {
                    sb.append("**").append(text).append("**");
                } else if (run.isItalic()) {
                    sb.append("_").append(text).append("_");
                } else {
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }

    private int drawFormattedText(Graphics2D g, String text, int x, int y, int maxWidth) {
        Font plainFont = new Font("Arial", Font.PLAIN, 14);
        Font boldFont = new Font("Arial", Font.BOLD, 14);
        Font italicFont = new Font("Arial", Font.ITALIC, 14);

        g.setFont(plainFont);
        FontMetrics metrics = g.getFontMetrics();
        int lineHeight = metrics.getHeight();

        String[] words = text.split("\\s+");
        int currentX = x;

        for (String word : words) {
            // Simple formatting detection
            boolean isBold = word.startsWith("**") && word.endsWith("**");
            boolean isItalic = word.startsWith("_") && word.endsWith("_");

            if (isBold) {
                word = word.substring(2, word.length() - 2);
                g.setFont(boldFont);
            } else if (isItalic) {
                word = word.substring(1, word.length() - 1);
                g.setFont(italicFont);
            } else {
                g.setFont(plainFont);
            }

            int wordWidth = metrics.stringWidth(word + " ");
            if (currentX + wordWidth > x + maxWidth) {
                currentX = x;
                y += lineHeight;
            }

            g.drawString(word, currentX, y);
            currentX += wordWidth;
        }

        return y + lineHeight; // Return the new y position
    }

    private byte[] generateGenericPreview(File file, String mimeType) throws IOException {
        return generateGenericPreview(getFileTypeDescription(mimeType), file.getName());
    }

    private byte[] generateGenericPreview(String fileType, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Draw background
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, 800, 800);

        // Draw file icon
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRoundRect(250, 200, 300, 300, 20, 20);

        // Draw file type text
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 48));
        drawCenteredString(graphics, fileType, 400, 350);

        // Draw file name
        graphics.setFont(new Font("Arial", Font.PLAIN, 24));
        graphics.setColor(Color.BLACK);
        drawCenteredString(graphics, fileName, 400, 600);

        graphics.dispose();
        return thumbnailService.convertToByteArray(image);
    }

    private void drawCenteredString(Graphics2D g, String text, int x, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x - textWidth / 2, y);
    }

    private String getFileTypeDescription(String mimeType) {
        if (mimeType.startsWith("text/"))
            return "Text File";
        if (mimeType.contains("zip") || mimeType.contains("compressed"))
            return "Archive";
        if (mimeType.contains("audio"))
            return "Audio File";
        if (mimeType.contains("video"))
            return "Video File";
        return "File";
    }

    // Implement the TextSegment class properly
    private static class TextSegment {
        String text;
        boolean bold;
        boolean italic;

        TextSegment(String text, boolean bold, boolean italic) {
            this.text = text;
            this.bold = bold;
            this.italic = italic;
        }
    }

    // Implement parseFormattedText method
    private List<TextSegment> parseFormattedText(String text) {
        List<TextSegment> segments = new ArrayList<>();
        // Simple implementation - can be enhanced
        segments.add(new TextSegment(text, false, false));
        return segments;
    }
}