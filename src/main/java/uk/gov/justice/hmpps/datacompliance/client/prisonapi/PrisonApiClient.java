package uk.gov.justice.hmpps.datacompliance.client.prisonapi;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffendersWithImagesResponse;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.IMAGE_JPEG;

@Service
public class PrisonApiClient {

    private static final String OFFENDER_IDS_PATH = "/api/offenders/ids";
    private static final String OFFENDERS_WITH_IMAGES_PATH = "/api/images/offenders";
    private static final String OFFENDER_IMAGE_METADATA_PATH = "/api/images/offenders/%s";
    private static final String IMAGE_DATA_PATH = "/api/images/%s/data";
    private static final String OFFENDER_PENDING_DELETIONS_PATH = "/api/data-compliance/offenders/pending-deletions";

    private final WebClient webClient;
    private final DataComplianceProperties dataComplianceProperties;

    public PrisonApiClient(@Qualifier("authorizedWebClient") final WebClient webClient,
                           final DataComplianceProperties dataComplianceProperties) {
        this.webClient = webClient;
        this.dataComplianceProperties = dataComplianceProperties;
    }

    public OffenderNumbersResponse getOffenderNumbers(final long offset, final long limit) {

        final var response = webClient.get()
            .uri(dataComplianceProperties.getPrisonApiBaseUrl() + OFFENDER_IDS_PATH)
            .header("Page-Offset", String.valueOf(offset))
            .header("Page-Limit", String.valueOf(limit))
            .retrieve()
            .toEntityList(OffenderNumber.class)
            .block();

        return offenderNumbersResponse(response);
    }

    public OffenderNumbersResponse getOffendersWithNewImages(final LocalDate lastRunDate,
                                                             final long pageNumber,
                                                             final long limit) {

        final var uri = UriComponentsBuilder.fromUriString(dataComplianceProperties.getPrisonApiBaseUrl())
            .path(OFFENDERS_WITH_IMAGES_PATH)
            .queryParam("fromDateTime", lastRunDate.atStartOfDay())
            .queryParam("paged", true)
            .queryParam("size", limit)
            .queryParam("page", pageNumber)
            .build().encode().toUri();

        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(OffendersWithImagesResponse.class)
            .map(response -> OffenderNumbersResponse.builder()
                .totalCount(response.getTotalElements())
                .offenderNumbers(new HashSet<>(response.getOffenderNumbers()))
                .build())
            .block();
    }

    public List<OffenderImageMetadata> getOffenderFaceImagesFor(final OffenderNumber offenderNumber) {

        final var url = dataComplianceProperties.getPrisonApiBaseUrl() +
            format(OFFENDER_IMAGE_METADATA_PATH, offenderNumber.getOffenderNumber());

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToFlux(OffenderImageMetadata.class)
            .onErrorResume(WebClientResponseException.class,
                ex -> NOT_FOUND.equals(ex.getStatusCode()) ? Mono.empty() : Mono.error(ex))
            .filter(OffenderImageMetadata::isOffenderFaceImage)
            .toStream().collect(toList());
    }

    public Optional<OffenderImage> getImageData(final OffenderNumber offenderNumber, final long imageId) {

        return webClient.get()
            .uri(dataComplianceProperties.getPrisonApiBaseUrl() + format(IMAGE_DATA_PATH, imageId))
            .accept(IMAGE_JPEG)
            .retrieve()
            .bodyToMono(byte[].class)
            .map(data -> OffenderImage.builder()
                .offenderNumber(offenderNumber)
                .imageId(imageId)
                .imageData(data)
                .build())

            // Handling edge case where image had no image data and a 404 response was returned
            .onErrorResume(WebClientResponseException.class,
                ex -> NOT_FOUND.equals(ex.getStatusCode()) ? Mono.empty() : Mono.error(ex))

            .blockOptional();
    }

    private OffenderNumbersResponse offenderNumbersResponse(final ResponseEntity<List<OffenderNumber>> response) {

        final var offenderNumbers = requireNonNull(response.getBody(), "No body found in response.");

        return OffenderNumbersResponse.builder()
            .totalCount(getTotalCountFrom(response))
            .offenderNumbers(new HashSet<>(offenderNumbers))
            .build();
    }

    @SuppressWarnings("rawtypes")
    private long getTotalCountFrom(final ResponseEntity entity) {

        final var totalCountHeader = Optional.ofNullable(entity.getHeaders())
            .map(headers -> headers.get("Total-Records"))
            .flatMap(headers -> headers.stream().findFirst());

        return totalCountHeader
            .map(Long::valueOf)
            .orElseThrow(() -> new IllegalStateException("Response did not contain Total-Records header"));
    }

    @Data
    @Builder
    public static class OffenderNumbersResponse {
        private long totalCount;
        private Set<OffenderNumber> offenderNumbers;
    }
}
