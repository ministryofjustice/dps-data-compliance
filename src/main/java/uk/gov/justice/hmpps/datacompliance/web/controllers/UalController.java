package uk.gov.justice.hmpps.datacompliance.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.hmpps.datacompliance.services.ual.UalReportService;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse;

import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;

@Slf4j
@RestController
@AllArgsConstructor
@Tag(name = "/ual")
@RequestMapping(value = "/ual", produces = APPLICATION_JSON_VALUE)
public class UalController {

    private final UalReportService reportService;

    @Operation(
        summary = "Retrieve unlawfully at large offender report data",
        description = "Used to retrieve unlawfully at large offender report data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request"),
    })
    @GetMapping(path = "/offender-data")
    public List<UalOffenderResponse> getUalOffenders() {
        return reportService.getUalOffenders();
    }


    @Operation(
        summary = "Upload a file containing offender unlawfully at large data.",
        description = "Update the unlawfully at large report. Uploads should adhere to the previously specified format. If a record does not yet exist, a new one will be created." +
            "If A record already exists, it will be updated")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No content"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request"),
    })
    @PutMapping(path = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUalReport(@RequestParam("file") final MultipartFile file) {
        return reportService.updateReport(file)
            .map(ignore -> created(URI.create("/ual/offender-data")).build())
            .orElse(noContent().build());
    }
}
