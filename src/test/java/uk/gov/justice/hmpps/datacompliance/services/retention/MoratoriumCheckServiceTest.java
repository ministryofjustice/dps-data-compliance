package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.CHILD_ABUSE_REGEX;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.CHILD_ABUSE_REGEX_REVERSED;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.RED_FLAGS;

@ExtendWith(MockitoExtension.class)
class MoratoriumCheckServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final long RETENTION_CHECK_ID = 1;

    @Mock
    private DataComplianceEventPusher eventPusher;

    private MoratoriumCheckService service;

    @BeforeEach
    void setUp() {
        service = new MoratoriumCheckService(eventPusher);
    }

    @Test
    void requestFreeTextSearch() {

        service.requestFreeTextSearch(OFFENDER_NUMBER, RETENTION_CHECK_ID);

        verify(eventPusher).requestFreeTextMoratoriumCheck(
                OFFENDER_NUMBER,
                RETENTION_CHECK_ID,
                List.of(RED_FLAGS, CHILD_ABUSE_REGEX, CHILD_ABUSE_REGEX_REVERSED));
    }

    @Test
    void checkRegexMatch() {
        assertThat("Some text flagging offender as a paedophile.".matches(RED_FLAGS)).isTrue();
        assertThat("Some text containing no 'red flags'.".matches(RED_FLAGS)).isFalse();

        assertThat("Some text containing child abuse evidence.".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing unrelated comments.".matches(CHILD_ABUSE_REGEX)).isFalse();

        assertThat("Some text mentioning abuse of children.".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing unrelated comments.".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();
    }
}
