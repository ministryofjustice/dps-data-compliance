package uk.gov.justice.hmpps.datacompliance.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.retention.ManualRetentionService;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionReasonDisplayName;
import uk.gov.justice.hmpps.datacompliance.web.dto.ManualRetentionRequest;

import javax.persistence.EntityNotFoundException;
import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.transform.ManualRetentionTransform.transform;

@RestController
@AllArgsConstructor
@Tag(name = "/retention")
@RequestMapping(value = "/retention", produces = APPLICATION_JSON_VALUE)
public class RetentionController {

    private final ManualRetentionService retentionService;

    @Operation(
        summary = "Get offender retention reasons",
        description = "List the potential reasons why an offender record can be kept beyond the standard period")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request"),
    })
    @GetMapping(path = "/offenders/retention-reasons")
    public List<ManualRetentionReasonDisplayName> getRetentionReasons() {
        return retentionService.getRetentionReasons();
    }

    @Operation(
        summary = "Get retention record",
        description = "Used to retrieve the retention record of an offender (including reasons why an offender record has been kept beyond the standard period)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Requested resource not found"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request"),
    })
    @GetMapping(path = "/offenders/{offenderNo}")
    public ResponseEntity<ManualRetention> getRetentionRecord(
        @Parameter(name = "offenderNo", required = true, example = "A1234BC")
        @NotEmpty
        @PathVariable("offenderNo") final String offenderNo) {
        return retentionService.findManualOffenderRetention(new OffenderNumber(offenderNo))
            .map(body -> ok().eTag(retentionService.getETag(body)).body(transform(body)))
            .orElseThrow(EntityNotFoundException::new);
    }

    @Operation(
        summary = "Update a retention record",
        description = "Update the reasons why an offender record should be kept beyond the standard period. If a record does not yet exist, a new one will be created.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "204", description = "No content"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Unrecoverable error occurred whilst processing request"),
    })
    @PutMapping(path = "/offenders/{offenderNo}")
    public ResponseEntity<?> updateRetentionRecord(final WebRequest webRequest,
                                                   @Parameter(name = "offenderNo", required = true, example = "A1234BC")
                                                   @NotEmpty @PathVariable("offenderNo") final String offenderNo,
                                                   @RequestBody ManualRetentionRequest manualRetentionRequest) {

        return retentionService.updateManualOffenderRetention(
                new OffenderNumber(offenderNo),
                manualRetentionRequest,
                webRequest.getHeader("If-Match"))

            .map(ignored -> noContent().build())

            .orElse(created(URI.create("/retention/offenders/" + offenderNo)).build());
    }
}
