package University.exam.service;

import University.exam.Entity.Paper;
import University.exam.Entity.Question;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

@Service
public class PaperParsingService {

    public List<Question> parsePaper(File file, Paper paper) throws IOException {
        String text = "";
        System.out.println("Attempting to parse file: " + file.getName());
        
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            text = extractTextFromPdf(file);
            System.out.println("Extracted PDF Text Length: " + text.length());
        } else if (file.getName().toLowerCase().endsWith(".docx")) {
            System.out.println("Word (.docx) file detected. Extracting text...");
            text = extractTextFromWord(file);
            System.out.println("Extracted Word Text Length: " + text.length());
        } else if (file.getName().toLowerCase().endsWith(".doc")) {
            System.out.println("Legacy Word (.doc) file detected. Converting to fallback.");
            text = ""; 
        }

        if (text == null || text.trim().isEmpty()) {
            System.out.println("WARNING: No text extracted from file.");
            return new ArrayList<>();
        }

        List<Question> questions = structureQuestions(text, paper);
        System.out.println("Parsed Questions Count: " + questions.size());
        return questions;
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromWord(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception e) {
            System.err.println("Error extracting from Word document: " + e.getMessage());
            return "";
        }
    }

    public List<Question> structureQuestions(String text, Paper paper) {
        List<Question> questions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        String currentGroup = "Q1";
        int groupIndex = 1;
        
        // 1. Group Header: Q -1, Q.1, Question 1
        Pattern groupPattern = Pattern.compile("(?i)^(Q|Question)\\s*[\\.\\-]?\\s*(\\d+)");
        // 2. Optional: OR at start
        Pattern orPattern = Pattern.compile("(?i)^(OR)\\b");

        String[] ignoreKeywords = {
            "instructions", "attempt all", "attempt any", "attempt questions", "compulsory",
            "figures on the right", "figures to the right", "draw the figures",
            "seat no", "seatno", "enrolment", "enrollment", "roll no", "rollno",
            "university", "subject name", "subject code", "course code", "duration", "total marks",
            "maximum marks", "date:", "date/", "draw diagrams", "draw graphs",
            "wherever necessary", "where necessary", "wherever required", "where required"
        };
        
        boolean extractionStarted = false;
        Question activeQuestion = null;
        boolean nextQuestionIsOptional = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.contains("******")) continue;

            // Skip lines that match page numbers/footers/headers
            if (trimmed.matches("(?i)^(?:Page\\s*\\d+\\s*of\\s*\\d+|\\d+\\s*of\\s*\\d+|Page\\s*\\d+|\\d+\\s*\\|\\s*Page|\\-\\s*\\d+\\s*\\-)$")) {
                continue;
            }

            // Normalize spacing
            trimmed = trimmed.replaceAll("[ \\t\\x0B\\f\\r]+", " ");

            // Check if the line is exactly "OR" (case-insensitive)
            if (trimmed.equalsIgnoreCase("OR")) {
                nextQuestionIsOptional = true;
                continue;
            }

            // Check if it is a group header
            Matcher groupMatcher = groupPattern.matcher(trimmed);
            boolean isGroupHeader = false;
            if (groupMatcher.find()) {
                if (!isNewQuestionStart(trimmed)) {
                    isGroupHeader = true;
                }
            }

            if (isGroupHeader) {
                // Finalize active question
                if (activeQuestion != null) {
                    finalizeQuestion(activeQuestion);
                    if (isValidQuestion(activeQuestion)) {
                        questions.add(activeQuestion);
                    }
                    activeQuestion = null;
                }
                
                groupIndex = Integer.parseInt(groupMatcher.group(2));
                currentGroup = "Q" + groupIndex;
                extractionStarted = true;
                continue;
            }

            // Rule 1: Ignore everything before the first valid group or valid question
            if (!extractionStarted) {
                if (isNewQuestionStart(trimmed)) {
                    extractionStarted = true;
                } else {
                    continue;
                }
            }

            // Rule 2: Ignore Instruction Lines
            boolean skipLine = false;
            String lowerLine = trimmed.toLowerCase();
            for (String keyword : ignoreKeywords) {
                if (lowerLine.contains(keyword)) {
                    skipLine = true;
                    break;
                }
            }
            if (skipLine) continue;

            // Check if it starts a new question
            if (isNewQuestionStart(trimmed)) {
                // Finalize previous question
                if (activeQuestion != null) {
                    finalizeQuestion(activeQuestion);
                    if (isValidQuestion(activeQuestion)) {
                        questions.add(activeQuestion);
                    }
                }

                // Start new question
                activeQuestion = new Question();
                activeQuestion.setPaper(paper);
                activeQuestion.setQuestionGroup(currentGroup);
                
                String questionText = trimmed;
                boolean isOptional = nextQuestionIsOptional;
                nextQuestionIsOptional = false;

                // Check for OR
                if (orPattern.matcher(trimmed).find()) {
                    isOptional = true;
                    questionText = trimmed.replaceAll("(?i)^OR\\s*", "");
                }

                activeQuestion.setText(questionText);
                activeQuestion.setOptional(isOptional);
            } else {
                // Continuation line
                if (activeQuestion != null) {
                    String currentText = activeQuestion.getText();
                    activeQuestion.setText(currentText + " " + trimmed);
                }
            }
        }

        // Finalize last question
        if (activeQuestion != null) {
            finalizeQuestion(activeQuestion);
            if (isValidQuestion(activeQuestion)) {
                questions.add(activeQuestion);
            }
        }

        // Post-process questions to automatically pair OR options
        int pairCounter = 1;
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (q.isOptional()) {
                if (i > 0) {
                    Question prev = questions.get(i - 1);
                    String pairId = prev.getPairId();
                    if (pairId == null || pairId.isEmpty()) {
                        pairId = "P" + pairCounter++;
                        prev.setOptional(true);
                        prev.setPairId(pairId);
                    }
                    q.setPairId(pairId);
                }
            }
        }

        if (!extractionStarted) {
            throw new RuntimeException("Invalid paper format");
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found");
        }

        return questions;
    }

    private boolean isNewQuestionStart(String trimmed) {
        if (trimmed.isEmpty()) return false;

        // 1. Starts with OR (case-insensitive, word boundary)
        Pattern orPattern = Pattern.compile("(?i)^(OR)\\b");
        if (orPattern.matcher(trimmed).find()) {
            if (trimmed.startsWith("{") || trimmed.startsWith("(")) {
                return false;
            }
            return true;
        }

        // 2. Starts with a Question Number Pattern (e.g. 1. , Q1. , Q.1. )
        Pattern questionNumPattern = Pattern.compile("^(?:(?:Q|Question)\\s*[\\.\\-]?\\s*)?\\d+[\\.\\)]\\s+.*", Pattern.CASE_INSENSITIVE);
        if (!questionNumPattern.matcher(trimmed).matches()) {
            return false;
        }

        // Check for False Question Creation rules (overrides):
        if (trimmed.startsWith("{") || trimmed.startsWith("(")) {
            return false;
        }

        String contentAfterNum = trimmed.replaceAll("^(?:(?:Q|Question)\\s*[\\.\\-]?\\s*)?\\d+[\\.\\)]\\s*", "").trim();
        if (contentAfterNum.isEmpty()) {
            return false;
        }

        if (contentAfterNum.startsWith("{") || contentAfterNum.startsWith("(")) {
            return false;
        }

        // Starts with mathematical expression operator
        if (contentAfterNum.matches("^[+\\-*/=<>≤≥≠≈±\\u00d7\\u00f7\\u2212\\u2260\\u2264\\u2265].*")) {
            return false;
        }

        return true;
    }

    private void finalizeQuestion(Question q) {
        if (q == null) return;
        String text = q.getText();
        if (text == null) return;
        text = text.trim();

        // Remove trailing page numbers/footers at the end of the text
        // e.g. "1|Page", "Page 1", "Page 1 of 5", "- 1 -", etc.
        text = text.replaceAll("(?i)\\b(?:Page\\s*\\d+\\s*of\\s*\\d+|\\d+\\s*of\\s*\\d+|Page\\s*\\d+|\\d+\\s*\\|\\s*Page)\\b\\s*$", "").trim();
        text = text.replaceAll("(?i)\\bPage\\b\\s*$", "").trim(); // in case "Page" gets separated or standalone
        text = text.replaceAll("\\-\\s*\\d+\\s*\\-\\s*$", "").trim(); // - 1 - style

        // Remove trailing punctuation (like ., ?, !, :, ;, -) at the very end of the string
        // but preserve closing brackets/parentheses if there are any.
        String cleanText = text.replaceAll("[\\.\\?\\!\\:\\;\\-\\s]+$", "").trim();

        // 1. Enclosed marks: e.g. [3], (3 Marks), [3 Mark]
        java.util.regex.Pattern patternEnclosed = java.util.regex.Pattern.compile("(.*?)\\s*[\\(\\[]\\s*(?:Marks|Mark|M)?\\s*(\\d+)\\s*(?:Marks|Mark|M)?\\s*[\\)\\]]$", java.util.regex.Pattern.CASE_INSENSITIVE);
        
        // 2. Enclosed label first: e.g. [Marks: 3], (Mark 3)
        java.util.regex.Pattern patternEnclosedLabelFirst = java.util.regex.Pattern.compile("(.*?)\\s*[\\(\\[]\\s*(?:Marks|Mark|M)\\s*[\\:\\-]?\\s*(\\d+)\\s*[\\)\\]]$", java.util.regex.Pattern.CASE_INSENSITIVE);
        
        // 3. Standalone trailing marks (requires at least one whitespace before the number/label)
        // e.g. "What is X? 3" or "What is X? 3 Marks" or "What is X? Marks 3"
        java.util.regex.Pattern patternStandalone = java.util.regex.Pattern.compile("(.*?)\\s+(?:(?:Marks|Mark|M)\\s*[\\:\\-]?\\s*(\\d+)|(\\d+)\\s*(?:Marks|Mark|M)?)$", java.util.regex.Pattern.CASE_INSENSITIVE);

        java.util.regex.Matcher m;

        m = patternEnclosed.matcher(cleanText);
        if (m.matches()) {
            double marks = Double.parseDouble(m.group(2));
            if (marks > 0 && marks <= 100) {
                q.setText(m.group(1).trim());
                q.setMarks(marks);
                return;
            }
        }

        m = patternEnclosedLabelFirst.matcher(cleanText);
        if (m.matches()) {
            double marks = Double.parseDouble(m.group(2));
            if (marks > 0 && marks <= 100) {
                q.setText(m.group(1).trim());
                q.setMarks(marks);
                return;
            }
        }

        m = patternStandalone.matcher(cleanText);
        if (m.matches()) {
            String numStr = m.group(2) != null ? m.group(2) : m.group(3);
            if (numStr != null) {
                double marks = Double.parseDouble(numStr);
                if (marks > 0 && marks <= 100) {
                    q.setText(m.group(1).trim());
                    q.setMarks(marks);
                    return;
                }
            }
        }

        // Default fallback if no valid marks pattern matched
        if (q.getMarks() == null) {
            q.setMarks(1.0);
        }
    }

    private boolean isValidQuestion(Question q) {
        if (q == null) return false;
        String text = q.getText();
        if (text == null) return false;
        text = text.trim();
        return text.length() > 3 && !text.matches("^\\d+$");
    }

}
