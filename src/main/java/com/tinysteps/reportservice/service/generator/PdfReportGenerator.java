package com.tinysteps.reportservice.service.generator;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.tinysteps.reportservice.model.AppointmentDto;
import com.tinysteps.reportservice.model.ReportRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class PdfReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

    @Override
    public void generateAppointmentReport(List<AppointmentDto> appointments, String outputPath, ReportRequestDto requestDto) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            
            document.open();
            
            // Add title
            Paragraph title = new Paragraph("Appointment Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Add report parameters
            addReportParameters(document, requestDto);
            
            // Add appointments table
            addAppointmentsTable(document, appointments);
            
            document.close();
            
            log.info("PDF report successfully generated at: {}", outputPath);
            
        } catch (Exception e) {
            log.error("Failed to create PDF file", e);
            throw new RuntimeException("Failed to create PDF file: " + e.getMessage(), e);
        }
    }

    private void addReportParameters(Document document, ReportRequestDto requestDto) throws DocumentException {
        Paragraph params = new Paragraph("Report Parameters:", HEADER_FONT);
        params.setSpacingAfter(10);
        document.add(params);
        
        if (requestDto.getStartDate() != null && requestDto.getEndDate() != null) {
            Paragraph dateRange = new Paragraph("Date Range: " + requestDto.getStartDate() + " to " + requestDto.getEndDate(), NORMAL_FONT);
            document.add(dateRange);
        }
        
        if (requestDto.getDoctorId() != null) {
            Paragraph doctorId = new Paragraph("Doctor ID: " + requestDto.getDoctorId(), NORMAL_FONT);
            document.add(doctorId);
        }
        
        if (requestDto.getPatientId() != null) {
            Paragraph patientId = new Paragraph("Patient ID: " + requestDto.getPatientId(), NORMAL_FONT);
            document.add(patientId);
        }
        
        if (requestDto.getBranchId() != null) {
            Paragraph branchId = new Paragraph("Branch ID: " + requestDto.getBranchId(), NORMAL_FONT);
            document.add(branchId);
        }
        
        // Add generated at timestamp
        Paragraph generatedAt = new Paragraph("Generated At: " + java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER), NORMAL_FONT);
        generatedAt.setSpacingAfter(20);
        document.add(generatedAt);
    }

    private void addAppointmentsTable(Document document, List<AppointmentDto> appointments) throws DocumentException {
        Paragraph tableTitle = new Paragraph("Appointments (" + appointments.size() + " total):", HEADER_FONT);
        tableTitle.setSpacingAfter(10);
        document.add(tableTitle);
        
        PdfPTable table = new PdfPTable(6); // 6 columns
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        // Add headers
        table.addCell(createHeaderCell("Appointment Number"));
        table.addCell(createHeaderCell("Patient"));
        table.addCell(createHeaderCell("Doctor"));
        table.addCell(createHeaderCell("Date & Time"));
        table.addCell(createHeaderCell("Status"));
        table.addCell(createHeaderCell("Notes"));
        
        // Add data rows
        for (AppointmentDto appointment : appointments) {
            table.addCell(createCell(appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : appointment.getId() != null ? appointment.getId() : "", false));
            table.addCell(createCell(appointment.getPatientName() != null ? appointment.getPatientName() : appointment.getPatientId() != null ? appointment.getPatientId() : "", false));
            table.addCell(createCell(appointment.getDoctorName() != null ? appointment.getDoctorName() : appointment.getDoctorId() != null ? appointment.getDoctorId() : "", false));
            table.addCell(createCell(appointment.getFormattedAppointmentTime(), false));
            table.addCell(createCell(appointment.getStatus() != null ? appointment.getStatus() : "N/A", false));
            table.addCell(createCell(appointment.getNotes() != null ? appointment.getNotes() : "", false));
        }
        
        document.add(table);
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell createCell(String text, boolean bold) {
        Font font = bold ? HEADER_FONT : NORMAL_FONT;
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }
}