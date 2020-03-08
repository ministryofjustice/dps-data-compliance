package uk.gov.justice.hmpps.datacompliance.web.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.hmpps.datacompliance.web.dto.ErrorResponse;
import uk.gov.justice.hmpps.datacompliance.web.dto.OffenderRetention;

import javax.validation.constraints.NotEmpty;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Api(tags = {"/retention"})
@RequestMapping(value = "/retention", produces = APPLICATION_JSON_VALUE)
public class RetentionController {

    @ApiOperation(
            value = "Get retention record",
            notes = "Get retention record",
            nickname = "getRetentionRecord" )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created", response = String.class),
            @ApiResponse(code = 400, message = "Invalid request.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Requested resource not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request.", response = ErrorResponse.class),
    })
    @GetMapping(path = "/offender/{offenderNo}")
    public OffenderRetention getRetentionRecord(
            @ApiParam(value = "offenderNo", required = true, example = "A1234BC")
            @NotEmpty
            @PathVariable("offenderNo") final String offenderNo) {
        return OffenderRetention.builder().build();
    }

}
