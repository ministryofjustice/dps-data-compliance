package uk.gov.justice.hmpps.datacompliance.web.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import uk.gov.justice.hmpps.datacompliance.web.dto.ErrorResponse;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(tags = {"/audit"})
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    public static final String DESTRUCTION_LOG_FILENAME = "NOMIS-DATA-DESTRUCTION-LOG_%s_%s.csv";
    public static final String RETAINED_OFFENDER_REASONS_FILENAME = "RETAINED_OFFENDER_REASONS_%s_%s.csv";

    private final AuditService auditService;
    private final CsvService csvService;
    private final UserSecurityUtils userSecurityUtils;
    private final TimeSource timeSource;

    @ApiOperation(
        value = "Get the destruction log",
        notes = "Get the destruction log for NOMIS offender data deletions",
        nickname = "getDestructionLog")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @GetMapping(path = "/destruction-log",
        produces = {APPLICATION_JSON_VALUE, "text/csv"})
    public ResponseEntity<?> getDestructionLog(@RequestBody Optional<Pageable> pageable, @RequestHeader(ACCEPT) String acceptHeader) throws JsonProcessingException {
        final var destructionLogResponse = auditService.retrieveDestructionLog(pageable.orElse(Pageable.unpaged()));

        return !isCsvRequested(acceptHeader) ? buildJsonResponse(destructionLogResponse)
            : buildCsvFileResponse(csvService.toCsv(destructionLogResponse), DESTRUCTION_LOG_FILENAME);
    }

    @ApiOperation(
        value = "Get retained offenders",
        notes = "Get the offenders which have been retained and the reasons for which they were retained",
        nickname = "getDestructionLog")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @GetMapping(path = "/retained-offenders",
        produces = {APPLICATION_JSON_VALUE, "text/csv"})
    public ResponseEntity<?> getRetainedOffenders(@RequestBody Optional<Pageable> pageable, @RequestHeader(ACCEPT) String acceptHeader, @RequestParam("filter") Optional<String> filter) throws JsonProcessingException {
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
