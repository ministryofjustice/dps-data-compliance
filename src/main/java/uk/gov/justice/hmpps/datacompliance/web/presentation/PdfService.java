package uk.gov.justice.hmpps.datacompliance.web.presentation;


import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.tagging.StandardRoles;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.UnitValue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;
import uk.gov.justice.hmpps.datacompliance.web.dto.DestructionLogResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PdfService {


    private final UserSecurityUtils userSecurityUtils;
    private final TimeSource timeSource;

    public ResponseEntity<byte[]> destructionLogAsPdf(List<DestructionLogResponse> destructionLog) throws IOException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            createPdf(destructionLog, outputStream);
            outputStream.close();
            return buildResponseEntity(outputStream);
        } catch (Exception e) {
            log.error("", e);
            throw e;
        }
    }


    private void createPdf(List<DestructionLogResponse> destructionLog, ByteArrayOutputStream outputStream) {
        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputStream));
        Document document = new Document(pdfDocument);
        document.add(title());
        document.add(description());
        document.add(table(destructionLog));
        document.close();
    }

    private Paragraph description() {
        Paragraph paragraph = new Paragraph();
        paragraph.add("Here is some text about the destruction log");
        return paragraph;
    }

    private Paragraph title() {
        Paragraph para = new Paragraph("Nomis Data Destruction Log")
            .setFontColor(new DeviceRgb(8, 73, 117))
            .setFontSize(20f)
            .setUnderline();
        para.getAccessibilityProperties().setRole(StandardRoles.H1);
        return para;
    }

    private Table table(List<DestructionLogResponse> destructionLog) {
        Table table = new Table(UnitValue.createPercentArray(8)).useAllAvailableWidth();
        createTableHeaders(table);
        addTableData(table,destructionLog);
        return table;
    }

    private void createTableHeaders(Table table) {

        table.addHeaderCell(headerCell("NOMIS ID", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("First Name", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("Middle Name", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("Last Name", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("Date of birth", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("Type of record destroyed", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("Method of destruction", 2)).useAllAvailableWidth();
        table.addHeaderCell(headerCell("Date of destruction", 2)).useAllAvailableWidth();

    }

    private void addTableData(Table table, List<DestructionLogResponse> destructionLog) {

        destructionLog.forEach(dl -> {
            table.addCell(dl.getNomisId());
            table.addCell(dl.getFirstName());
            table.addCell(dl.getMiddleName());
            table.addCell(dl.getLastName());
            table.addCell(dl.getDateOfBirth().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
            table.addCell(dl.getTypeOfRecordDestroyed());
            table.addCell(dl.getMethodOfDestruction());
            table.addCell(dl.getDestructionDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
        });
    }

    private Cell headerCell(String headerText, int width) {
        return new Cell(width, 1).add(new Paragraph(headerText));
    }

    private ResponseEntity<byte[]> buildResponseEntity(ByteArrayOutputStream outputStream) {
        String filename = "NOMIS-DATA-DESTRUCTION-LOG" + "_" + timeSource.nowAsLocalDate().toString() + "_" + userSecurityUtils.getCurrentUsername();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        headers.setContentDispositionFormData("filename", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        ResponseEntity<byte[]> response = new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
        return response;
    }
}
