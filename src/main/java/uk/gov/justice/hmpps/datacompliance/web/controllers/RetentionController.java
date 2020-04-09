package uk.gov.justice.hmpps.datacompliance.web.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import uk.gov.justice.hmpps.datacompliance.web.dto.ErrorResponse;
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
@Api(tags = {"/retention"})
@RequestMapping(value = "/retention", produces = APPLICATION_JSON_VALUE)
public class RetentionController {

    private final ManualRetentionService retentionService;

    @ApiOperation(
            value = "Get offender retention reasons",
            notes = "List the potential reasons why an offender record can be kept beyond the standard period",
            nickname = "getRetentionReasons" )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @GetMapping(path = "/offenders/retention-reasons")
    public List<ManualRetentionReasonDisplayName> getRetentionReasons() {
        return retentionService.getRetentionReasons();
    }

    @ApiOperation(
            value = "Get retention record",
            notes = "Used to retrieve the retention record of an offender (including reasons why an offender record has been kept beyond the standard period)",
            nickname = "getRetentionRecord" )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class),
            @ApiResponse(code = 404, message = "Requested resource not found", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @GetMapping(path = "/offenders/{offenderNo}")
    public ResponseEntity<ManualRetention> getRetentionRecord(
            @ApiParam(value = "offenderNo", required = true, example = "A1234BC")
            @NotEmpty
            @PathVariable("offenderNo") final String offenderNo) {
        return retentionService.findManualOffenderRetention(new OffenderNumber(offenderNo))
                .map(body -> ok().eTag(retentionService.getETag(body)).body(transform(body)))
                .orElseThrow(EntityNotFoundException::new);
    }

    @ApiOperation(
            value = "Update a retention record",
            notes = "Update the reasons why an offender record should be kept beyond the standard period. If a record does not yet exist, a new one will be created.",
            nickname = "updateRetentionRecord" )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created", response = String.class),
            @ApiResponse(code = 204, message = "No content", response = String.class),
            @ApiResponse(code = 400, message = "Invalid request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @PutMapping(path = "/offenders/{offenderNo}")
    public ResponseEntity<?> updateRetentionRecord(final WebRequest webRequest,
                                                   @ApiParam(value = "offenderNo", required = true, example = "A1234BC")
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
