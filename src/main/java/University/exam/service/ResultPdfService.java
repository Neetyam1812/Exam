package University.exam.service;

import University.exam.Entity.Result;
import University.exam.Entity.Student;
import University.exam.repository.StudentRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ResultPdfService {

    @Autowired
    private StudentRepository studentRepository;

    public void generateResultPdf(HttpServletResponse response, List<Result> results, String subject, String semester, String division) throws IOException {
        // Use 36pt (0.5 inch) margins for a clean spreadsheet/marksheet export layout
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Determine dynamic marks header (e.g., Marks (50) or Marks (100))
        String marksHeader = "Marks";
        if (results != null && !results.isEmpty()) {
            Double tm = results.get(0).getTotalMarks();
            if (tm != null) {
                if (tm % 1 == 0) {
                    marksHeader = "Marks (" + tm.intValue() + ")";
                } else {
                    marksHeader = "Marks (" + tm + ")";
                }
            }
        }

        // Table with 6 columns: Roll No, GR No, Student Name, Marks, Status, Remarks
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2.0f, 3.8f, 1.5f, 2.0f, 3.0f});

        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font tableBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        // Header cells
        String[] headers = {"Roll No", "GR No", "Student Name", marksHeader, "Status", "Remarks"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
            cell.setBackgroundColor(new java.awt.Color(240, 240, 240));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        int seqRollNo = 1;
        for (Result r : results) {
            String rollNoStr = "-";
            Student student = null;
            if (r.getSubmission() != null) {
                student = r.getSubmission().getStudent();
            }
            if (student == null && r.getEnrollmentNo() != null) {
                student = studentRepository.findByEnrollmentNo(r.getEnrollmentNo()).orElse(null);
            }

            if (student != null && student.getRollNo() != null) {
                String div = student.getDivision() != null ? student.getDivision().trim().toUpperCase() : "";
                rollNoStr = div + String.format("%02d", student.getRollNo());
            } else if (r.getDivision() != null) {
                String div = r.getDivision().trim().toUpperCase();
                rollNoStr = div + String.format("%02d", seqRollNo++);
            } else {
                rollNoStr = String.format("%02d", seqRollNo++);
            }

            String enrollNo = r.getEnrollmentNo() != null ? r.getEnrollmentNo() : "-";
            String sName = r.getStudentName() != null ? r.getStudentName().toUpperCase() : "-";

            String marksStr = "-";
            String statusStr = r.getResultStatus() != null ? r.getResultStatus() : "PENDING";
            String remarksStr = "";

            if ("TERMINATED".equals(statusStr)) {
                marksStr = "0 / " + (r.getTotalMarks() != null ? String.valueOf(r.getTotalMarks().intValue()) : "100");
                remarksStr = r.getTerminationReason() != null ? r.getTerminationReason() : "Exam terminated before submission.";
            } else if ("DISQUALIFIED".equals(statusStr)) {
                marksStr = "0 / " + (r.getTotalMarks() != null ? String.valueOf(r.getTotalMarks().intValue()) : "100");
                remarksStr = r.getTerminationReason() != null ? r.getTerminationReason() : "Exam terminated due to proctoring violation.";
            } else if ("ABSENT".equals(statusStr)) {
                marksStr = "0 / " + (r.getTotalMarks() != null ? String.valueOf(r.getTotalMarks().intValue()) : "100");
                remarksStr = "Absent from examination.";
            } else {
                double obtained = r.getObtainedMarks() != null ? r.getObtainedMarks() : 0.0;
                double total = r.getTotalMarks() != null ? r.getTotalMarks() : 100.0;
                marksStr = ((obtained % 1 == 0) ? String.valueOf((int) obtained) : String.valueOf(obtained))
                        + " / " + ((total % 1 == 0) ? String.valueOf((int) total) : String.valueOf(total));
                remarksStr = "PASSED".equals(statusStr) ? "Passed" : ("FAILED".equals(statusStr) ? "Failed" : "");
            }

            // Cell 1: Roll No (Centered)
            PdfPCell cell = new PdfPCell(new Phrase(rollNoStr, tableBodyFont));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);

            // Cell 2: GR No (Centered)
            cell = new PdfPCell(new Phrase(enrollNo, tableBodyFont));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);

            // Cell 3: Student Name (Left-aligned)
            cell = new PdfPCell(new Phrase(sName, tableBodyFont));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);

            // Cell 4: Marks (Centered)
            cell = new PdfPCell(new Phrase(marksStr, tableBodyFont));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);

            // Cell 5: Status (Centered)
            cell = new PdfPCell(new Phrase(statusStr, tableBodyFont));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);

            // Cell 6: Remarks (Left-aligned)
            cell = new PdfPCell(new Phrase(remarksStr, tableBodyFont));
            cell.setBorderColor(new java.awt.Color(180, 180, 180));
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }

        document.add(table);
        document.close();
    }
}
