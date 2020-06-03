package uk.gov.justice.hmpps.datacompliance.client.image.recognition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.rekognition.model.QualityFilter.HIGH;
import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.*;

@ExtendWith(MockitoExtension.class)
class AwsImageRecognitionClientTest {

    private static final byte[] DATA = new byte[] { (byte) 0x01 };
    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final long OFFENDER_IMAGE_ID = 1L;
    private static final String EXPECTED_FACE_ID = "face1";
    private static final String COLLECTION_NAME = "collection_name";
    private static final float SIMILARITY_THRESHOLD = 99.9f;

    @Mock
    private RekognitionClient awsClient;

    private ImageRecognitionClient client;

    @BeforeEach
    void setUp() {
        client = new AwsImageRecognitionClient(awsClient, COLLECTION_NAME, SIMILARITY_THRESHOLD);
    }

    @Test
    void uploadImageToCollection() {

        var request = ArgumentCaptor.forClass(IndexFacesRequest.class);

        when(awsClient.indexFaces(request.capture())).thenReturn(indexedFaces(EXPECTED_FACE_ID));

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID).get().getFaceId())
                .isEqualTo(EXPECTED_FACE_ID);

        assertThat(request.getValue().maxFaces()).isEqualTo(2);
        assertThat(request.getValue().qualityFilter()).isEqualTo(HIGH);
        assertThat(request.getValue().collectionId()).isEqualTo(COLLECTION_NAME);
        assertThat(request.getValue().externalImageId()).isEqualTo(OFFENDER_NUMBER.getOffenderNumber() + "-" + OFFENDER_IMAGE_ID);
        assertThat(request.getValue().image().bytes().asByteArray()).isEqualTo(DATA);
    }

    @Test
    void uploadImageToCollectionEnsuresOnlyOneFaceIndexed() {

        when(awsClient.indexFaces(any(IndexFacesRequest.class)))
                .thenReturn(indexedFaces(EXPECTED_FACE_ID, "another-face-in-the-image"));

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID).getError())
                .isEqualTo(MULTIPLE_FACES_FOUND);

        verify(awsClient).deleteFaces(DeleteFacesRequest.builder()
                .collectionId(COLLECTION_NAME)
                .faceIds(EXPECTED_FACE_ID)
                .build());
        verify(awsClient).deleteFaces(DeleteFacesRequest.builder()
                .collectionId(COLLECTION_NAME)
                .faceIds("another-face-in-the-image")
                .build());
    }

    @Test
    void uploadImageToCollectionHandlesImageWithNoFace() {

        when(awsClient.indexFaces(any(IndexFacesRequest.class)))
                .thenReturn(indexedFaces(/* NONE */));

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID).getError())
                .isEqualTo(FACE_NOT_FOUND);
    }

    @Test
    void uploadImageToCollectionHandlesImageWithPoorQualityFace() {

        when(awsClient.indexFaces(any(IndexFacesRequest.class)))
                .thenReturn(indexedFacesBuilder(/* NONE */)
                        .unindexedFaces(UnindexedFace.builder().build())
                        .build());

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID).getError())
                .isEqualTo(FACE_POOR_QUALITY);
    }

    @Test
    void findMatches() {

        var request = ArgumentCaptor.forClass(SearchFacesRequest.class);

        when(awsClient.searchFaces(request.capture())).thenReturn(matchingFace());

        assertThat(client.findMatchesFor(new FaceId("someFace")))
                .extracting(FaceId::getFaceId).contains(EXPECTED_FACE_ID);
        assertThat(request.getValue().collectionId()).isEqualTo(COLLECTION_NAME);
        assertThat(request.getValue().faceId()).isEqualTo("someFace");
        assertThat(request.getValue().faceMatchThreshold()).isEqualTo(SIMILARITY_THRESHOLD);
    }

    @Test
    void findMatchesReturnsNoResults() {

        when(awsClient.searchFaces(any(SearchFacesRequest.class))).thenReturn(noMatchingFace());

        assertThat(client.findMatchesFor(new FaceId("someFace"))).isEmpty();
    }

    private IndexFacesResponse indexedFaces(final String ... faceIds) {
        return indexedFacesBuilder(faceIds).build();
    }

    private IndexFacesResponse.Builder indexedFacesBuilder(final String ... faceIds) {
        return IndexFacesResponse.builder()
                .faceRecords(stream(faceIds)
                        .map(id -> FaceRecord.builder()
                                .face(Face.builder()
                                        .faceId(id)
                                        .build())
                                .build())
                        .collect(toList()));
    }

    private SearchFacesResponse matchingFace() {
        return SearchFacesResponse.builder()
                .faceMatches(FaceMatch.builder()
                        .face(Face.builder()
                                .faceId(EXPECTED_FACE_ID)
                                .build())
                        .build())
                .build();
    }

    private SearchFacesResponse noMatchingFace() {
        return SearchFacesResponse.builder().build();
    }
}
