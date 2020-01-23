package uk.gov.justice.hmpps.datacompliance.services.client.image.recognition;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsImageRecognitionClientTest {

    private static final byte[] DATA = new byte[] { (byte) 0x01 };
    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("offender1");
    private static final long OFFENDER_IMAGE_ID = 1L;
    private static final String EXPECTED_FACE_ID = "face1";
    private static final String COLLECTION_NAME = "collection_name";

    @Mock
    private AmazonRekognitionClient awsClient;

    private ImageRecognitionClient client;

    @BeforeEach
    void setUp() {
        client = new AwsImageRecognitionClient(awsClient, COLLECTION_NAME);
    }

    @Test
    void uploadImageToCollection() {

        var request = ArgumentCaptor.forClass(IndexFacesRequest.class);

        when(awsClient.indexFaces(request.capture())).thenReturn(indexedFaces(EXPECTED_FACE_ID));

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID))
                .contains(EXPECTED_FACE_ID);

        assertThat(request.getValue().getMaxFaces()).isEqualTo(2);
        assertThat(request.getValue().getQualityFilter()).isEqualTo("HIGH");
        assertThat(request.getValue().getCollectionId()).isEqualTo(COLLECTION_NAME);
        assertThat(request.getValue().getExternalImageId()).isEqualTo(OFFENDER_NUMBER.getOffenderNumber() + "-" + OFFENDER_IMAGE_ID);
        assertThat(request.getValue().getImage().getBytes().array()).isEqualTo(DATA);
    }

    @Test
    void uploadImageToCollectionEnsuresOnlyOneFaceIndexed() {

        when(awsClient.indexFaces(any()))
                .thenReturn(indexedFaces(EXPECTED_FACE_ID, "another-face-in-the-image"));

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID))
                .isEmpty();

        verify(awsClient).deleteFaces(new DeleteFacesRequest()
                .withCollectionId(COLLECTION_NAME)
                .withFaceIds(EXPECTED_FACE_ID));
        verify(awsClient).deleteFaces(new DeleteFacesRequest()
                .withCollectionId(COLLECTION_NAME)
                .withFaceIds("another-face-in-the-image"));
    }

    @Test
    void uploadImageToCollectionHandlesImageWithNoFace() {

        when(awsClient.indexFaces(any()))
                .thenReturn(indexedFaces(/* NONE */));

        assertThat(client.uploadImageToCollection(DATA, OFFENDER_NUMBER, OFFENDER_IMAGE_ID))
                .isEmpty();
    }

    private IndexFacesResult indexedFaces(final String ... faceIds) {
        final var result = new IndexFacesResult();

        result.withFaceRecords(stream(faceIds)
                .map(id -> new FaceRecord().withFace(new Face().withFaceId(id)))
                .toArray(FaceRecord[]::new));

        return result;
    }
}