package uk.gov.justice.hmpps.datacompliance.services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderDeletionEventPusherTest {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private SnsAsyncClient client;

    private OffenderDeletionEventPusher eventPusher;

    @BeforeEach
    void setUp() {
        eventPusher = new OffenderDeletionAwsEventPusher(client, "topic.arn", OBJECT_MAPPER);
    }

    @Test
    void sendEvent() {

        ArgumentCaptor<PublishRequest> request = ArgumentCaptor.forClass(PublishRequest.class);
        when(client.publish(request.capture()))
                .thenReturn(completedFuture(PublishResponse.builder().messageId("message1").build()));

        eventPusher.sendEvent("offender1");

        assertThat(request.getValue().message()).contains("offender1");
    }
}