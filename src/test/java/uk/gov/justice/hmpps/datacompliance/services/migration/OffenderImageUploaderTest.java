package uk.gov.justice.hmpps.datacompliance.services.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.image.recognition.ImageRecognitionClient;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderImageUploaderTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("offender1");
    private static final long IMAGE_ID = 123L;
    private static final byte[] IMAGE_DATA = new byte[]{0x12};

    @Mock
    private Elite2ApiClient elite2ApiClient;

    @Mock
    private ImageRecognitionClient imageRecognitionClient;

    private OffenderImageUploader imageUploader;

    @BeforeEach
    void setUp() {
        imageUploader = new OffenderImageUploader(elite2ApiClient, imageRecognitionClient);
    }

    @Test
    void uploadOffenderImages() {

        when(elite2ApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(new OffenderImageMetadata(IMAGE_ID, "FACE")));

        when(elite2ApiClient.getImageData(IMAGE_ID)).thenReturn(IMAGE_DATA);

        imageUploader.accept(OFFENDER_NUMBER);

        verify(imageRecognitionClient).uploadImageToCollection(IMAGE_DATA, OFFENDER_NUMBER, IMAGE_ID);
    }

    @Test
    void name() {

    }
}