package com.techpool.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

@Service
public class PreviewService {
    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private final FileStorageService fileStorageService;
    private final ThumbnailService thumbnailService;

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
            } else if (mimeType.contains("word")) {
                try {
                    return generateWordPreview(file);
                } catch (NoClassDefFoundError e) {
                    log.error("POI library not available for DOCX processing", e);
                    return generateGenericPreview("DOCX Preview Unavailable", file.getName());
                }
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

    private byte[] generateErrorPreview(String message) throws IOException {
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

    private byte[] generateOfficePreview(File file, String mimeType) throws IOException {
        if (mimeType.contains("word")) {
            return generateWordPreview(file);
        } else if (mimeType.contains("excel")) {
            return generateExcelPreview(file);
        } else if (mimeType.contains("powerpoint")) {
            return generatePowerPointPreview(file);
        }
        return generateGenericPreview(file, mimeType);
    }

    // Add this if you want to implement Excel preview later
    private byte[] generateExcelPreview(File file) throws IOException {
        // TODO: Implement using Apache POI XSSF
        return generateGenericPreview("Spreadsheet", file.getName());
    }

    // Add this if you want to implement PowerPoint preview later
    private byte[] generatePowerPointPreview(File file) throws IOException {
        // TODO: Implement using Apache POI XSLF
        return generateGenericPreview("Presentation", file.getName());
    }

    private byte[] generateWordPreview(File file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                // Check paragraph style first
                String style = p.getStyle();
                boolean isHeading = style != null && (style.contains("Heading") || style.contains("Title"));

                // Extract text with runs
                for (XWPFRun run : p.getRuns()) {
                    String text = run.text();
                    if (text == null || text.isEmpty())
                        continue;

                    if (isHeading) {
                        content.append("**HEADING**").append(text).append("**HEADING**");
                    } else if (run.isBold()) {
                        content.append("**BOLD**").append(text).append("**BOLD**");
                    } else if (run.isItalic()) {
                        content.append("_ITALIC_").append(text).append("_ITALIC_");
                    } else {
                        content.append(text);
                    }
                }
                content.append("\n");

                if (content.length() > 3000)
                    break;
            }
            return createTextPreviewImage("DOCUMENT PREVIEW", content.toString());
        }
    }

    private byte[] createTextPreviewImage(String title, String content) throws IOException {
        BufferedImage image = new BufferedImage(800, 1000, BufferedImage.TYPE_INT_RGB); // Increased height
        Graphics2D g = image.createGraphics();

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 1000);

        // Draw title
        g.setColor(new Color(0, 0, 128)); // Dark blue
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString(title, 50, 50);

        // Draw content
        int y = 100;
        for (String line : content.split("\n")) {
            if (y > 950)
                break; // Adjusted for taller image

            // Handle formatted text
            if (line.contains("**HEADING**")) {
                String heading = line.replace("**HEADING**", "");
                g.setColor(new Color(0, 0, 150));
                g.setFont(new Font("Arial", Font.BOLD, 18));
                g.drawString(heading, 40, y);
                y += 25;
            } else if (line.contains("**BULLET**")) {
                String bullet = line.replace("**BULLET**", "").trim();
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.PLAIN, 14));
                g.drawString("• " + bullet, 60, y);
                y += 20;
            } else if (line.contains("**BOLD**")) {
                String boldText = line.replace("**BOLD**", "");
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString(boldText, 50, y);
                y += 18;
            } else {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Arial", Font.PLAIN, 14));

                // Word wrapping for long lines
                if (line.length() > 90) {
                    String[] words = line.split(" ");
                    StringBuilder currentLine = new StringBuilder();
                    for (String word : words) {
                        if (currentLine.length() + word.length() > 90) {
                            g.drawString(currentLine.toString(), 50, y);
                            y += 18;
                            currentLine = new StringBuilder();
                        }
                        currentLine.append(word).append(" ");
                    }
                    g.drawString(currentLine.toString(), 50, y);
                } else {
                    g.drawString(line, 50, y);
                }
                y += 18;
            }
        }

        g.dispose();
        return thumbnailService.convertToByteArray(image);
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

    private String detectMimeType(File file) throws IOException {
        String mimeType = Files.probeContentType(file.toPath());
        if (mimeType == null) {
            mimeType = new Tika().detect(file);
        }
        return mimeType;
    }
}