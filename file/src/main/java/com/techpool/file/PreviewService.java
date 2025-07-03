package com.techpool.file;

import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader; // Add this import
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.techpool.file.util.FileTypeHandler;
import com.techpool.file.util.FileTypeHandlerFactory;
import com.techpool.file.util.LibreOfficeHelper;

import org.springframework.core.io.Resource;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.tika.Tika;

@Service
public class PreviewService {
    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private final FileStorageService storageService;
    private final FileTypeHandlerFactory handlerFactory;
    private final Tika tika = new Tika();

    public PreviewService(FileStorageService storageService, FileTypeHandlerFactory handlerFactory) {
        this.storageService = storageService;
        this.handlerFactory = handlerFactory;
    }

    public byte[] generatePreview(String filename) {
        try {
            Resource fileResource = storageService.loadFileAsResource(filename);
            File file = fileResource.getFile();

            if (!file.exists()) {
                throw new FileNotFoundException("File not found in storage");
            }

            String mimeType = tika.detect(file);
            FileTypeHandler handler = handlerFactory.getHandler(mimeType);

            if (handler == null) {
                throw new IllegalArgumentException("No handler for mimeType: " + mimeType);
            }

            return handler.generatePreview(file);
        } catch (Exception e) {
            log.error("Preview failed for {}", filename, e);
            return generateErrorPreview("Preview unavailable: " + e.getMessage());
        }
    }

    public byte[] generateErrorPreview(String message) {
        try {
            BufferedImage image = new BufferedImage(600, 200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // White background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 600, 200);

            // Red error border
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(5));
            g.drawRect(10, 10, 580, 180);

            // Error text
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));

            // Word wrap for long messages
            FontMetrics fm = g.getFontMetrics();
            int lineHeight = fm.getHeight();
            int y = 50;

            for (String line : message.split("\n")) {
                for (String part : splitStringEvery(line, 60)) {
                    g.drawString(part, 30, y);
                    y += lineHeight;
                }
            }

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Critical error during error preview generation", e);
            return new byte[0];
        }
    }

    private List<String> splitStringEvery(String s, int interval) {
        List<String> result = new ArrayList<>();
        int length = s.length();
        for (int i = 0; i < length; i += interval) {
            result.add(s.substring(i, Math.min(length, i + interval)));
        }
        return result;
    }

    public byte[] generateMultiPagePreview(String fileName) {
        try {
            Resource fileResource = storageService.loadFileAsResource(fileName);
            File file = fileResource.getFile();
            String mimeType = tika.detect(file);

            if (mimeType.contains("pdf")) {
                return generatePdfMultiPagePreview(file);
            } else if (mimeType.contains("word") || mimeType.contains("officedocument")) {
                return generateOfficeMultiPagePreview(file);
            }
            return generatePreview(fileName); // Fallback for non-multi-page files
        } catch (Exception e) {
            log.error("Multi-page preview failed for {}", fileName, e);
            return generateErrorPreview("Preview generation failed");
        }
    }

    private byte[] generatePdfMultiPagePreview(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            // Configurable number of pages to render (default: 3)
            int pagesToRender = Math.min(pageCount, getMaxPreviewPages());

            List<BufferedImage> pages = new ArrayList<>();

            // Render configured number of pages
            for (int i = 0; i < pagesToRender; i++) {
                pages.add(renderer.renderImage(i, 1.0f)); // 1.0 = 100 DPI
            }

            return combinePages(pages, file);
        }
    }

    // Get max pages from configuration
    private int getMaxPreviewPages() {
        // Read from application.properties or use default
        return 3; // Or get from @Value("${preview.max-pages:3}")
    }

    public byte[] validateImage(byte[] imageData) throws IOException {
        // Check basic PNG signature
        if (imageData.length < 8 ||
                !(imageData[0] == (byte) 0x89 &&
                        imageData[1] == 'P' &&
                        imageData[2] == 'N' &&
                        imageData[3] == 'G')) {
            throw new IOException("Invalid PNG image data");
        }
        return imageData;
    }

    private byte[] generateOfficeMultiPagePreview(File file) throws Exception {
        List<BufferedImage> pages = LibreOfficeHelper.convertToImages(file, getLibreOfficePath());
        return combinePages(pages, file);
    }

    private byte[] combinePages(List<BufferedImage> pages, File file) throws IOException {
        if (pages.isEmpty()) {
            return generateErrorPreview("No pages found");
        }

        // Calculate dimensions
        int width = pages.stream()
                .mapToInt(BufferedImage::getWidth)
                .max()
                .orElse(800);
        int spacing = 20;
        int totalHeight = pages.stream()
                .mapToInt(img -> img.getHeight())
                .sum() + (pages.size() * spacing);

        // Create combined image
        BufferedImage combined = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = combined.createGraphics();

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, totalHeight);

        // Draw pages with page numbers
        int y = 0;
        for (int i = 0; i < pages.size(); i++) {
            BufferedImage page = pages.get(i);

            // Center each page horizontally
            int x = (width - page.getWidth()) / 2;
            g.drawImage(page, x, y, null);

            // Add page number
            g.setColor(new Color(0, 0, 0, 150)); // Semi-transparent black
            g.fillRect(x, y + page.getHeight() - 30, 50, 20);
            g.setColor(Color.WHITE);
            g.drawString("Page " + (i + 1), x + 5, y + page.getHeight() - 15);

            y += page.getHeight() + spacing;
        }
        g.dispose();

        // Convert to PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }

    // private String generateQrContent(File file) throws IOException {
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // return String.format(
    // "File: %s\nSize: %d KB\nType: %s\nUploaded: %s",
    // file.getName(),
    // file.length() / 1024,
    // Files.probeContentType(file.toPath()),
    // sdf.format(new Date()));
    // }

    private String getLibreOfficePath() {
        // Configure this from your application properties
        return "C:\\LibreOfficePortable\\App\\LibreOffice\\program\\soffice.exe";
    }

    // private byte[] generatePdfPreview(File file) throws IOException {
    // try (PDDocument document = PDDocument.load(file)) {
    // PDFRenderer renderer = new PDFRenderer(document);
    // BufferedImage image = renderer.renderImageWithDPI(0, 100);
    // BufferedImage resizedImage = thumbnailService.resizeImage(image, 800, 800);
    // // Add QR code
    // BufferedImage finalImage = addQrCodeToPreview(resizedImage, file);
    // return thumbnailService.convertToByteArray(resizedImage);
    // }
    // }

    // private BufferedImage addQrCodeToPreview(BufferedImage originalImage, File
    // file) {
    // try {
    // // Generate QR code content - customize this as needed
    // String qrContent = "File: " + file.getName() + "\n" +
    // "Size: " + (file.length() / 1024) + " KB\n" +
    // "Preview generated on: " + new java.util.Date();

    // // Generate QR code
    // BufferedImage qrCode = QrCodeUtil.generateQrCode(qrContent, 150);

    // // Create new image with space for QR code
    // BufferedImage combined = new BufferedImage(
    // originalImage.getWidth(),
    // originalImage.getHeight() + qrCode.getHeight() + 20,
    // BufferedImage.TYPE_INT_RGB);

    // Graphics2D g = combined.createGraphics();

    // // Draw original image
    // g.drawImage(originalImage, 0, 0, null);

    // // Draw QR code at bottom
    // int qrX = (originalImage.getWidth() - qrCode.getWidth()) / 2;
    // int qrY = originalImage.getHeight() + 10;
    // g.drawImage(qrCode, qrX, qrY, null);

    // // Add text below QR code
    // g.setColor(Color.BLACK);
    // g.setFont(new Font("Arial", Font.PLAIN, 12));
    // String qrText = "Scan for file information";
    // int textWidth = g.getFontMetrics().stringWidth(qrText);
    // g.drawString(qrText, (originalImage.getWidth() - textWidth) / 2, qrY +
    // qrCode.getHeight() + 15);

    // g.dispose();
    // return combined;
    // } catch (Exception e) {
    // log.warn("Failed to add QR code to preview", e);
    // return originalImage; // Return original if QR fails
    // }
    // }

    // private byte[] generateWordPreview(File file) {
    // try {
    // return generateWordPreviewWithLibreOffice(file);
    // } catch (Exception e) {
    // // log.warn("Docx4j conversion failed, falling back to text preview", e);
    // log.warn("LibreOffice conversion failed, falling back to text preview", e);
    // try {
    // return generateWordPreviewWithPoi(file);
    // } catch (Exception ex) {
    // // log.error("Both docx4j and POI failed", ex);
    // log.error("Both LibreOffice and POI failed", ex);
    // try {
    // return generateErrorPreview("Preview unavailable");
    // } catch (IOException ioex) {
    // throw new RuntimeException("Failed to generate error preview", ioex);
    // }
    // }
    // }
    // }

    // public byte[] generateWordPreviewWithLibreOffice(File file) throws Exception
    // {
    // Path tempDir = Files.createTempDirectory("lo-preview-");
    // try {
    // String sofficePath = getLibreOfficePath();
    // log.info("Attempting conversion with LibreOffice at: {}", sofficePath);

    // ProcessBuilder pb = new ProcessBuilder(
    // sofficePath,
    // "--headless",
    // "--convert-to", "png",
    // "--outdir", tempDir.toString(),
    // file.getAbsolutePath());

    // // Redirect all output to log file
    // File logFile = tempDir.resolve("conversion.log").toFile();
    // pb.redirectErrorStream(true);
    // pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

    // Process process = pb.start();
    // boolean success = process.waitFor(2, TimeUnit.MINUTES);

    // // Read the log file regardless of success
    // String logContent = Files.readString(logFile.toPath());
    // log.info("LibreOffice conversion log:\n{}", logContent);

    // if (!success) {
    // process.destroyForcibly();
    // throw new IOException("Conversion timed out after 2 minutes");
    // }

    // if (process.exitValue() != 0) {
    // throw new IOException("LibreOffice failed with exit code " +
    // process.exitValue() + "\nLogs:\n" + logContent);
    // }

    // // Find the generated PNG
    // try (Stream<Path> files = Files.list(tempDir)) {
    // Optional<Path> pngFile = files
    // .filter(p -> p.toString().toLowerCase().endsWith(".png"))
    // .findFirst();

    // if (pngFile.isEmpty()) {
    // throw new IOException("No PNG file generated. Logs:\n" + logContent);
    // }

    // BufferedImage image = ImageIO.read(pngFile.get().toFile());
    // if (image == null) {
    // throw new IOException("Generated PNG is invalid");
    // }

    // return thumbnailService.convertToByteArray(
    // thumbnailService.resizeImage(image, 800, 800));
    // }
    // } catch (Exception e) {
    // log.error("LibreOffice conversion failed", e);
    // throw e;
    // } finally {
    // try {
    // FileUtils.deleteDirectory(tempDir.toFile());
    // } catch (IOException e) {
    // log.warn("Failed to clean temp directory: {}", tempDir, e);
    // }
    // }
    // }

    // private byte[] generateExcelPreview(File file) throws IOException {
    // try {
    // // First try with LibreOffice
    // return generateExcelPreviewWithLibreOffice(file);
    // } catch (Exception e) {
    // log.warn("LibreOffice Excel conversion failed, falling back to generic
    // preview", e);
    // try {
    // return generateExcelPreviewWithPoi(file);
    // } catch (Exception ex) {
    // log.error("Both LibreOffice and POI failed for Excel preview", ex);
    // try {
    // return generateGenericPreview("Excel File", file.getName());
    // } catch (IOException ioex) {
    // throw new RuntimeException("Failed to generate Excel preview", ioex);
    // }
    // }
    // }
    // }

    // private byte[] generateExcelPreviewWithLibreOffice(File file) throws
    // Exception {
    // // Similar to Word conversion but for Excel files
    // Path tempDir = Files.createTempDirectory("lo-excel-preview-");
    // try {
    // String sofficePath = getLibreOfficePath();
    // log.info("Attempting Excel conversion with LibreOffice at: {}", sofficePath);

    // ProcessBuilder pb = new ProcessBuilder(
    // sofficePath,
    // "--headless",
    // "--convert-to", "png",
    // "--outdir", tempDir.toString(),
    // file.getAbsolutePath());

    // File logFile = tempDir.resolve("excel_conversion.log").toFile();
    // pb.redirectErrorStream(true);
    // pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

    // Process process = pb.start();
    // boolean success = process.waitFor(libreOfficeTimeout, TimeUnit.MILLISECONDS);

    // String logContent = Files.readString(logFile.toPath());
    // log.info("LibreOffice Excel conversion log:\n{}", logContent);

    // if (!success) {
    // process.destroyForcibly();
    // throw new IOException("Excel conversion timed out");
    // }

    // if (process.exitValue() != 0) {
    // throw new IOException("LibreOffice failed with exit code " +
    // process.exitValue() + "\nLogs:\n" + logContent);
    // }

    // try (Stream<Path> files = Files.list(tempDir)) {
    // Optional<Path> pngFile = files
    // .filter(p -> p.toString().toLowerCase().endsWith(".png"))
    // .findFirst();

    // if (pngFile.isEmpty()) {
    // throw new IOException("No PNG file generated for Excel. Logs:\n" +
    // logContent);
    // }

    // BufferedImage image = ImageIO.read(pngFile.get().toFile());
    // if (image == null) {
    // throw new IOException("Generated Excel PNG is invalid");
    // }

    // return thumbnailService.convertToByteArray(
    // thumbnailService.resizeImage(image, 800, 800));
    // }
    // } finally {
    // try {
    // FileUtils.deleteDirectory(tempDir.toFile());
    // } catch (IOException e) {
    // log.warn("Failed to clean temp directory: {}", tempDir, e);
    // }
    // }
    // }

    // private byte[] generateExcelPreviewWithPoi(File file) throws IOException {
    // // Create a simple preview showing spreadsheet info
    // BufferedImage image = new BufferedImage(800, 800,
    // BufferedImage.TYPE_INT_RGB);
    // Graphics2D g = image.createGraphics();

    // // White background
    // g.setColor(Color.WHITE);
    // g.fillRect(0, 0, 800, 800);

    // // Title
    // g.setColor(Color.BLUE);
    // g.setFont(new Font("Arial", Font.BOLD, 24));
    // g.drawString("Excel Spreadsheet Preview", 50, 50);

    // // Basic info
    // g.setColor(Color.BLACK);
    // g.setFont(new Font("Arial", Font.PLAIN, 16));

    // try (Workbook workbook = WorkbookFactory.create(file)) {
    // int y = 100;
    // g.drawString("File: " + file.getName(), 50, y);
    // y += 30;

    // for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
    // Sheet sheet = workbook.getSheetAt(i);
    // g.drawString("Sheet " + (i + 1) + ": " + sheet.getSheetName(), 50, y);
    // y += 20;

    // if (y > 700)
    // break; // Don't overflow the image
    // }
    // }

    // g.dispose();
    // return thumbnailService.convertToByteArray(image);
    // }

    // private byte[] generateCsvPreview(File file) throws IOException {
    // // Create a text-based preview of the CSV
    // BufferedImage image = new BufferedImage(800, 800,
    // BufferedImage.TYPE_INT_RGB);
    // Graphics2D g = image.createGraphics();

    // // White background
    // g.setColor(Color.WHITE);
    // g.fillRect(0, 0, 800, 800);

    // // Title
    // g.setColor(Color.BLUE);
    // g.setFont(new Font("Arial", Font.BOLD, 24));
    // g.drawString("CSV File Preview", 50, 50);

    // // Read first few lines
    // g.setColor(Color.BLACK);
    // g.setFont(new Font("Arial", Font.PLAIN, 14));

    // int y = 100;
    // int lineCount = 0;
    // int maxLines = 30;

    // try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
    // String line;
    // while ((line = reader.readLine()) != null && lineCount < maxLines) {
    // if (line.length() > 100) {
    // line = line.substring(0, 100) + "...";
    // }
    // g.drawString(line, 50, y);
    // y += 20;
    // lineCount++;

    // if (y > 750)
    // break;
    // }
    // }

    // if (lineCount == maxLines) {
    // g.drawString("... (more content not shown)", 50, y);
    // }

    // g.dispose();
    // return thumbnailService.convertToByteArray(image);
    // }

    // private byte[] generateXmlPreview(File file) throws IOException {
    // // Create a formatted preview of XML content
    // BufferedImage image = new BufferedImage(800, 800,
    // BufferedImage.TYPE_INT_RGB);
    // Graphics2D g = image.createGraphics();

    // // Dark background for better code viewing
    // g.setColor(new Color(30, 30, 30));
    // g.fillRect(0, 0, 800, 800);

    // // Title
    // g.setColor(Color.WHITE);
    // g.setFont(new Font("Arial", Font.BOLD, 24));
    // g.drawString("XML File Preview", 50, 50);

    // // Read and format XML content
    // g.setFont(new Font("Courier New", Font.PLAIN, 12));

    // int y = 100;
    // int lineCount = 0;
    // int maxLines = 30;

    // try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
    // String line;
    // while ((line = reader.readLine()) != null && lineCount < maxLines) {
    // line = line.trim();
    // if (line.isEmpty())
    // continue;

    // // Simple syntax highlighting
    // if (line.startsWith("<")) {
    // g.setColor(new Color(86, 156, 214)); // Blue for tags
    // } else {
    // g.setColor(Color.WHITE);
    // }

    // if (line.length() > 100) {
    // line = line.substring(0, 100) + "...";
    // }

    // g.drawString(line, 50, y);
    // y += 20;
    // lineCount++;

    // if (y > 750)
    // break;
    // }
    // }

    // if (lineCount == maxLines) {
    // g.setColor(Color.LIGHT_GRAY);
    // g.drawString("... (more content not shown)", 50, y);
    // }

    // g.dispose();
    // return thumbnailService.convertToByteArray(image);
    // }

    // // Helper: Detect LibreOffice path based on OS
    // private String getLibreOfficePath() throws IOException {
    // String customPath =
    // "C:\\LibreOfficePortable\\App\\LibreOffice\\program\\soffice.exe";
    // File loFile = new File(customPath);

    // if (!loFile.exists()) {
    // throw new IOException("LibreOffice not found at: " + customPath +
    // "\nPlease verify installation path or install LibreOffice Portable");
    // }
    // return customPath;
    // }

    // private byte[] generateWordPreviewWithPoi(File file) throws IOException {
    // try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
    // BufferedImage image = new BufferedImage(1200, 1600,
    // BufferedImage.TYPE_INT_RGB);
    // Graphics2D g = image.createGraphics();

    // // White background
    // g.setColor(Color.WHITE);
    // g.fillRect(0, 0, 1200, 1600);

    // // Draw title
    // g.setColor(new Color(0, 0, 128));
    // g.setFont(new Font("Arial", Font.BOLD, 28));
    // g.drawString("DOCUMENT PREVIEW", 50, 50);

    // int y = 100;

    // // Process all pictures in document first
    // for (XWPFPictureData picture : doc.getAllPictures()) {
    // try {
    // BufferedImage img = ImageIO.read(new
    // ByteArrayInputStream(picture.getData()));
    // if (img != null) {
    // double scaleFactor = 1000.0 / img.getWidth();
    // int newHeight = (int) (img.getHeight() * scaleFactor);
    // g.drawImage(img, 100, y, 1000, newHeight, null);
    // y += newHeight + 20;
    // }
    // } catch (Exception e) {
    // log.warn("Failed to process image", e);
    // }
    // }

    // // Process paragraphs
    // for (XWPFParagraph p : doc.getParagraphs()) {
    // String text = extractFormattedText(p);
    // if (!text.trim().isEmpty()) {
    // y = drawFormattedText(g, text, 100, y, 1000);
    // y += 20; // Add space between paragraphs
    // }

    // if (y > 1500)
    // break;
    // }

    // g.dispose();

    // // Crop unused space
    // BufferedImage cropped = image.getSubimage(0, 0, 1200, Math.min(y + 50,
    // 1600));
    // return thumbnailService.convertToByteArray(cropped);
    // }
    // }

    // private String detectMimeType(File file) throws IOException {
    // String mimeType = Files.probeContentType(file.toPath());
    // if (mimeType == null) {
    // Tika tika = new Tika();
    // mimeType = tika.detect(file);
    // }
    // return mimeType;
    // }

    // private String extractFormattedText(XWPFParagraph p) {
    // StringBuilder sb = new StringBuilder();
    // for (XWPFRun run : p.getRuns()) {
    // String text = run.text();
    // if (text != null && !text.trim().isEmpty()) {
    // if (run.isBold()) {
    // sb.append("**").append(text).append("**");
    // } else if (run.isItalic()) {
    // sb.append("_").append(text).append("_");
    // } else {
    // sb.append(text);
    // }
    // }
    // }
    // return sb.toString();
    // }

    // private int drawFormattedText(Graphics2D g, String text, int x, int y, int
    // maxWidth) {
    // Font plainFont = new Font("Arial", Font.PLAIN, 14);
    // Font boldFont = new Font("Arial", Font.BOLD, 14);
    // Font italicFont = new Font("Arial", Font.ITALIC, 14);

    // g.setFont(plainFont);
    // FontMetrics metrics = g.getFontMetrics();
    // int lineHeight = metrics.getHeight();

    // String[] words = text.split("\\s+");
    // int currentX = x;

    // for (String word : words) {
    // // Simple formatting detection
    // boolean isBold = word.startsWith("**") && word.endsWith("**");
    // boolean isItalic = word.startsWith("_") && word.endsWith("_");

    // if (isBold) {
    // word = word.substring(2, word.length() - 2);
    // g.setFont(boldFont);
    // } else if (isItalic) {
    // word = word.substring(1, word.length() - 1);
    // g.setFont(italicFont);
    // } else {
    // g.setFont(plainFont);
    // }

    // int wordWidth = metrics.stringWidth(word + " ");
    // if (currentX + wordWidth > x + maxWidth) {
    // currentX = x;
    // y += lineHeight;
    // }

    // g.drawString(word, currentX, y);
    // currentX += wordWidth;
    // }

    // return y + lineHeight; // Return the new y position
    // }

    // private byte[] generateGenericPreview(File file, String mimeType) throws
    // IOException {
    // return generateGenericPreview(getFileTypeDescription(mimeType),
    // file.getName());
    // }

    // private byte[] generateGenericPreview(String fileType, String fileName)
    // throws IOException {
    // BufferedImage image = new BufferedImage(800, 800,
    // BufferedImage.TYPE_INT_RGB);
    // Graphics2D graphics = image.createGraphics();

    // // Draw background
    // graphics.setColor(Color.LIGHT_GRAY);
    // graphics.fillRect(0, 0, 800, 800);

    // // Draw file icon
    // graphics.setColor(Color.DARK_GRAY);
    // graphics.fillRoundRect(250, 200, 300, 300, 20, 20);

    // // Draw file type text
    // graphics.setColor(Color.WHITE);
    // graphics.setFont(new Font("Arial", Font.BOLD, 48));
    // drawCenteredString(graphics, fileType, 400, 350);

    // // Draw file name
    // graphics.setFont(new Font("Arial", Font.PLAIN, 24));
    // graphics.setColor(Color.BLACK);
    // drawCenteredString(graphics, fileName, 400, 600);

    // graphics.dispose();
    // return thumbnailService.convertToByteArray(image);
    // }

    // private void drawCenteredString(Graphics2D g, String text, int x, int y) {
    // int textWidth = g.getFontMetrics().stringWidth(text);
    // g.drawString(text, x - textWidth / 2, y);
    // }

    // private String getFileTypeDescription(String mimeType) {
    // if (mimeType.startsWith("text/"))
    // return "Text File";
    // if (mimeType.contains("zip") || mimeType.contains("compressed"))
    // return "Archive";
    // if (mimeType.contains("audio"))
    // return "Audio File";
    // if (mimeType.contains("video"))
    // return "Video File";
    // return "File";
    // }

}