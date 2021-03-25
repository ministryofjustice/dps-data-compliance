package uk.gov.justice.hmpps.datacompliance.web.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import uk.gov.justice.hmpps.datacompliance.services.ual.UalService;
import uk.gov.justice.hmpps.datacompliance.web.dto.ErrorResponse;
import uk.gov.justice.hmpps.datacompliance.web.dto.UalOffenderResponse;

import java.net.URI;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;

@Slf4j
@RestController
@AllArgsConstructor
@Api(tags = {"/ual"})
@RequestMapping(value = "/ual", produces = APPLICATION_JSON_VALUE)
public class UalController {

    private final UalService ualService;

    @ApiOperation(
        value = "Retrieve unlawfully at large offender report data",
        notes = "Used to retrieve unlawfully at large offender report data",
        nickname = "getUalOffenders" )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @GetMapping(path = "/offender-data")
    public List<UalOffenderResponse> getUalOffenders() {
        return ualService.getUalOffenders();
    }


    @ApiOperation(
            value = "Upload a file containing offender unlawfully at large data.",
            notes = "Update the unlawfully at large report. Uploads should adhere to the previously specified format. If a record does not yet exist, a new one will be created." +
                "If A record already exists, it will be updated",
            nickname = "updateUalReport" )
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No content", response = String.class),
            @ApiResponse(code = 400, message = "Invalid request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Unrecoverable error occurred whilst processing request", response = ErrorResponse.class),
    })
    @PutMapping(path = "/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadUalReport(@RequestParam("file") final MultipartFile file) {
        return ualService.updateReport(file)
            .map(ignore -> created(URI.create("/ual/offender-data")).build())
            .orElse(noContent().build());
    }
}
