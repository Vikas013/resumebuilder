package com.projectx.resumebuilder;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {

    public String extractTextFromPdf(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    public void createPdf(String content, String outputPath) throws IOException {
        // Create a new PDF document
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);  // Use A4 page size
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.setLeading(16f);  // Increased line spacing for readability

            float margin = 50;
            float width = page.getMediaBox().getWidth() - 2 * margin;  // Available width between left and right margins
            float yPosition = page.getMediaBox().getHeight() - margin;  // Start writing 50 units from top

            // Split content into lines
            String[] lines = content.split("\n");

            for (String line : lines) {
                // Check if the line is a heading
                if (isHeading(line)) {
                    // Add bold and underline styling for headings
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14.25f);  // Bold for main headings
                    yPosition = writeWrappedText(contentStream, line, margin, yPosition, width, true);  // Pass true to indicate heading
                    addHeadingUnderline(contentStream, line, margin, yPosition, page);
                    yPosition -= 20;  // Add space after heading and underline
                } else if (isSubHeading(line)) {
                    // For sub-headings like Role, Responsibilities, Duration
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);  // Shorter bold text for sub-headings
                    yPosition = writeWrappedText(contentStream, line, margin, yPosition, width, false);
                    yPosition -= 5;  // Space after sub-heading
                } else {
                    // Reset font for normal content
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    yPosition = writeFormattedText(contentStream, line, margin, yPosition, width);  // Handle bold text in normal lines
                }

                // Ensure we add new pages when the content exceeds the current page's height
                if (yPosition < margin) {
                    contentStream.close();  // Close the current content stream

                    // Add a new page
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.setLeading(16f);
                    yPosition = page.getMediaBox().getHeight() - margin;
                }
            }

            contentStream.close();  // Close the content stream
            document.save(outputPath);  // Save the document to the specified path
        }
    }

    // Utility method to check if a line is a heading
    private boolean isHeading(String line) {
        return line.matches("(?i)^(NAME|PROFESSIONAL SUMMARY|TECHNICAL SKILLS|Primary Skill Sets|Secondary Skill Sets|WORK EXPERIENCE|Client#\\d+).*");
    }

    // Utility method to check if a line is a sub-heading (like Role, Responsibilities, Duration)
    private boolean isSubHeading(String line) {
        return line.matches("(?i)^(Role:|Duration:|Responsibilities:).*");
    }

    // Method to write formatted text handling bold tags
    private float writeFormattedText(PDPageContentStream contentStream, String text, float xStart, float yStart, float width) throws IOException {
        String[] parts = text.split("\\*\\*");  // Split by '**' to identify bold text

        float currentY = yStart;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i % 2 == 1) {
                // Bold text for odd indexed parts (the ones between **)
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
            } else {
                // Regular text for even indexed parts
                contentStream.setFont(PDType1Font.HELVETICA, 12);
            }

            // Write the part
            currentY = writeWrappedText(contentStream, part, xStart, currentY, width, false);
        }

        return currentY;  // Return the updated yPosition
    }
    // Method to write text with word wrapping
    private float writeWrappedText(PDPageContentStream contentStream, String text, float xStart, float yStart, float width, boolean isHeading) throws IOException {
        List<String> lines = getWrappedLines(text, width, isHeading ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, isHeading ? 14 : 12);
        for (String line : lines) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xStart, yStart);
            contentStream.showText(line);
            contentStream.endText();
            yStart -= 14.5f;  // Move down after each line
        }
        return yStart;
    }

    // Method to calculate the wrapped lines
    private List<String> getWrappedLines(String text, float width, PDType1Font font, float fontSize) throws IOException {
        List<String> wrappedLines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Check if the current word can fit in the current line
            String tempLine = currentLine + (currentLine.length() == 0 ? "" : " ") + word;

            // Measure the width of the current line
            float textWidth = font.getStringWidth(tempLine) / 1000 * fontSize;

            if (textWidth > width) {
                // If the current line exceeds the available width, add the current line to the list
                wrappedLines.add(currentLine.toString());
                // Start a new line with the current word
                currentLine = new StringBuilder(word);
            } else {
                // If the word fits in the line, append it
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            }
        }

        // Add the last line if there is remaining text
        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }

        return wrappedLines;
    }

    // Method to add an underline under a heading
    private void addHeadingUnderline(PDPageContentStream contentStream, String heading, float margin, float yPosition, PDPage page) throws IOException {
        float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(heading) / 1000 * 14;  // Calculate text width
        float underlineYPosition = yPosition - 6;  // Move the underline further down from the text

        contentStream.moveTo(margin, underlineYPosition);  // Move below the text for the line
        contentStream.lineTo(margin + textWidth, underlineYPosition);  // Draw a line under the text
        contentStream.stroke();  // Draw the line
    }
}
