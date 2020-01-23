package uk.gov.justice.hmpps.datacompliance.services.client.elite2api;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.*;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Service
public class Elite2ApiClient {

    private static final String OFFENDER_IDS_PATH = "/api/offenders/ids";
    private static final String OFFENDER_IMAGE_METADATA_PATH = "/api/images/offenders/%s";
    private static final String IMAGE_DATA_PATH = "/api/images/%s/data";

    private final WebClient webClient;
    private final DataComplianceProperties dataComplianceProperties;

    public Elite2ApiClient(@Qualifier("authorizedWebClient") final WebClient webClient,
                           final DataComplianceProperties dataComplianceProperties) {
        this.webClient = webClient;
        this.dataComplianceProperties = dataComplianceProperties;
    }

    public OffenderNumbersResponse getOffenderNumbers(final long offset, final long limit) {

        final var response = webClient.get()
                .uri(dataComplianceProperties.getElite2ApiBaseUrl() + OFFENDER_IDS_PATH)
                .header("Page-Offset", String.valueOf(offset))
                .header("Page-Limit", String.valueOf(limit))
                .attributes(clientRegistrationId("dps-data-compliance"))
                .retrieve()
                .toEntityList(OffenderNumber.class)
                .block();

        return offenderNumbersResponse(response);
    }

    public List<OffenderImageMetadata> getOffenderFaceImagesFor(final OffenderNumber offenderNumber) {

        final var url = dataComplianceProperties.getElite2ApiBaseUrl() +
                format(OFFENDER_IMAGE_METADATA_PATH, offenderNumber.getOffenderNumber());

        return webClient.get()
                .uri(url)
                .attributes(clientRegistrationId("dps-data-compliance"))
                .retrieve()
                .bodyToFlux(OffenderImageMetadata.class)
                .filter(OffenderImageMetadata::isOffenderFaceImage)
                .toStream().collect(toList());
    }

    public byte[] getImageData(final long imageId) {

        return webClient.get()
                .uri(dataComplianceProperties.getElite2ApiBaseUrl() + format(IMAGE_DATA_PATH, imageId))
                .attributes(clientRegistrationId("dps-data-compliance"))
                .accept(IMAGE_JPEG)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
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
