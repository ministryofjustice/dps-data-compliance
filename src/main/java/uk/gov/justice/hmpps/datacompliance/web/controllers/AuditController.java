package uk.gov.justice.hmpps.datacompliance.web.controllers;

import lombok.AllArgsConstructor;
import org.dom4j.DocumentException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.hmpps.datacompliance.services.audit.AuditService;
import uk.gov.justice.hmpps.datacompliance.web.dto.DestructionLogResponse;
import uk.gov.justice.hmpps.datacompliance.web.presentation.PdfService;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@RestController
@RequestMapping("/audit")
@AllArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final PdfService pdfService;

    @GetMapping(path = "/destruction-log", produces = APPLICATION_JSON_VALUE)
    public List<DestructionLogResponse> getDestructionLog(){
        return auditService.retrieveDestructionLog();
    }

    @GetMapping(path = "/destruction-log/pdf", produces = APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getDestructionLogAsPdf() throws IOException, DocumentException, DocumentException {
        return pdfService.destructionLogAsPdf(auditService.retrieveDestructionLog());

    }
}
