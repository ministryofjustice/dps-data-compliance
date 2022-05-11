package uk.gov.justice.hmpps.datacompliance.web.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.hmpps.datacompliance.security.UserSecurityUtils;
import uk.gov.justice.hmpps.datacompliance.services.audit.AuditService;
import uk.gov.justice.hmpps.datacompliance.services.audit.CsvService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "/audit")
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    public static final String DESTRUCTION_LOG_FILENAME = "NOMIS-DATA-DESTRUCTION-LOG_%s_%s.csv";
    public static final String RETAINED_OFFENDER_REASONS_FILENAME = "RETAINED_OFFENDER_REASONS_%s_%s.csv";

    private final AuditService auditService;
    private final CsvService csvService;
    private final UserSecurityUtils userSecurityUtils;
    private final TimeSource timeSource;

    @Operation(
        summary = "Get the destruction log",
        description = "Get the destruction log for NOMIS offender data deletions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request"),
    })
    @GetMapping(path = "/destruction-log",
        produces = {APPLICATION_JSON_VALUE, "text/csv"})
    public ResponseEntity<?> getDestructionLog(@RequestBody Optional<Pageable> pageable, @RequestHeader(ACCEPT) String acceptHeader) throws JsonProcessingException {
        final var destructionLogResponse = auditService.retrieveDestructionLog(pageable.orElse(Pageable.unpaged()));

        return !isCsvRequested(acceptHeader) ? buildJsonResponse(destructionLogResponse)
            : buildCsvFileResponse(csvService.toCsv(destructionLogResponse), DESTRUCTION_LOG_FILENAME);
    }

    @Operation(
        summary = "Get retained offenders",
        description = """
            Get the offenders which have been retained and their respective retention reasons.
            Update the request 'Accept' Header to 'text/csv' for a csv file response.""")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request")
    })
    @GetMapping(path = "/retained-offenders",
        produces = {APPLICATION_JSON_VALUE, "text/csv"})
    public ResponseEntity<?> getRetainedOffenders(@RequestBody Optional<Pageable> pageable,
                                                  @RequestHeader(ACCEPT) String acceptHeader,
                                                  @Parameter(name = "filter", description = "Filter the results to a subset", example = "duplicates")
                                                  @RequestParam("filter") Optional<String> filter) throws JsonProcessingException {
        final var retainedOffenderResponse = filter.isPresent() && equalsIgnoreCase(filter.get(), "duplicates")
            ? auditService.retrieveRetainedOffenderDuplicates(pageable.orElse(Pageable.unpaged()))
            : auditService.retrieveRetainedOffenders(pageable.orElse(Pageable.unpaged()));

        return !isCsvRequested(acceptHeader) ? buildJsonResponse(retainedOffenderResponse)
            : buildCsvFileResponse(csvService.toCsv(retainedOffenderResponse), RETAINED_OFFENDER_REASONS_FILENAME);
    }


    private ResponseEntity<Page<?>> buildJsonResponse(final Page<?> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }


    private ResponseEntity<byte[]> buildCsvFileResponse(final byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("filename", filename.formatted(timeSource.nowAsLocalDate().toString(), userSecurityUtils.getCurrentUsername().orElse("dps-compliance-user")));
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private boolean isCsvRequested(final String acceptHeader) {
        return !isBlank(acceptHeader) && acceptHeader.equalsIgnoreCase("text/csv");
    }


}
