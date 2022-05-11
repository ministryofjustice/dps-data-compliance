package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import java.util.List;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.CHILD_ABUSE_REGEX;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.CHILD_ABUSE_REGEX_REVERSED;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.PUBLIC_ROLE_ABUSE_REGEX;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.PUBLIC_ROLE_ABUSE_REGEX_REVERSED;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.RED_FLAGS;

@ExtendWith(MockitoExtension.class)
class MoratoriumCheckServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final long RETENTION_CHECK_ID = 1;
    private static final int MAX_REGEX_LENGTH = 512;

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
            List.of(RED_FLAGS,
                CHILD_ABUSE_REGEX,
                CHILD_ABUSE_REGEX_REVERSED,
                PUBLIC_ROLE_ABUSE_REGEX,
                PUBLIC_ROLE_ABUSE_REGEX_REVERSED));
    }

    @Test
    void regexLengthBelowMaximum() {
        assertThat(RED_FLAGS.length()).isLessThan(MAX_REGEX_LENGTH);
        assertThat(CHILD_ABUSE_REGEX.length()).isLessThan(MAX_REGEX_LENGTH);
        assertThat(CHILD_ABUSE_REGEX_REVERSED.length()).isLessThan(MAX_REGEX_LENGTH);
        assertThat(PUBLIC_ROLE_ABUSE_REGEX.length()).isLessThan(MAX_REGEX_LENGTH);
        assertThat(PUBLIC_ROLE_ABUSE_REGEX_REVERSED.length()).isLessThan(MAX_REGEX_LENGTH);
    }

    @Test
    void checkRegexMatch() {
        assertThat("Some text flagging offender as a paedophile.".matches(RED_FLAGS)).isTrue();
        assertThat("Some text containing no 'red flags'.".matches(RED_FLAGS)).isFalse();

        assertThat("Some text containing child abuse evidence.".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing unrelated comments.".matches(CHILD_ABUSE_REGEX)).isFalse();

        assertThat("Some text mentioning abuse of children.".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing unrelated comments.".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text mentioning schools and abuse.".matches(PUBLIC_ROLE_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing unrelated comments.".matches(PUBLIC_ROLE_ABUSE_REGEX)).isFalse();

        assertThat("Some text mentioning abuse in schools.".matches(PUBLIC_ROLE_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing unrelated comments.".matches(PUBLIC_ROLE_ABUSE_REGEX_REVERSED)).isFalse();
    }

    @Test
    void avoidFalseRegexMatchOnTheWordSon() {
        assertThat("Some text containing words risk and son within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text with different placement of words risk and son".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text with words surrounded by non-alphabetic characters:risk... and ...son!".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text with risk or reason of accidentally flagging".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words son and risk".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text with words surrounded by non-alphabetic characters:son... and ...risk!".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text with reasonable risk of accidentally flagging".matches(CHILD_ABUSE_REGEX)).isFalse();
    }

    @Test
    void avoidFalseRegexMatchOnBoyAndGirl() {
        assertThat("Some text containing words risk and girl within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and girl".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and ...girl;".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and girlfriend".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words risk and boy within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and boy".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and ...boy;".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and boyfriend".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words girl and risk".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words girlfriend risk".matches(CHILD_ABUSE_REGEX)).isFalse();

        assertThat("Some text containing words boy and risk".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words boyfriend risk".matches(CHILD_ABUSE_REGEX)).isFalse();
    }

    @Test
    void avoidFalseRegexMatchOnMinor() {
        assertThat("Some text containing words risk and minor within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and minor".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and ...minor;".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and minority".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words minor and risk".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words minority and risk".matches(CHILD_ABUSE_REGEX)).isFalse();
    }

    @Test
    void avoidFalseRegexMatchOnSchool() {
        assertThat("Some text containing words risk and school within it".matches(PUBLIC_ROLE_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and school".matches(PUBLIC_ROLE_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and ...school;".matches(PUBLIC_ROLE_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and schooling".matches(PUBLIC_ROLE_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words school and risk".matches(PUBLIC_ROLE_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words schooling and risk".matches(PUBLIC_ROLE_ABUSE_REGEX)).isFalse();
    }

    @Test
    void avoidFalseRegexMatchOnTeen() {
        assertThat("Some text containing words risk and teenager within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and teen".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and ...teen;".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words risk and canteen".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words teen and risk".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words teenager and risk".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words canteen and risk".matches(CHILD_ABUSE_REGEX)).isFalse();
    }

    @Test
    void avoidFalseRegexMatchOnTouch() {
        assertThat("Some text containing words touch and child within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing words touching and child within it".matches(CHILD_ABUSE_REGEX_REVERSED)).isTrue();
        assertThat("Some text containing phrases in touch and child".matches(CHILD_ABUSE_REGEX_REVERSED)).isFalse();

        assertThat("Some text containing words child and touching within it".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words child and touch".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing words child and ...touch;".matches(CHILD_ABUSE_REGEX)).isTrue();
        assertThat("Some text containing phrases child and in touch".matches(CHILD_ABUSE_REGEX)).isFalse();
    }

    @Test
    void retainDueToOffence() {
        assertThat(service.retainDueToOffence(offenderWithOffenceCodes("SX56099"))).isTrue();
        assertThat(service.retainDueToOffence(offenderWithOffenceCodes("SX56100", "SX56050", "NOT A MATCH"))).isTrue();
        assertThat(service.retainDueToOffence(offenderWithOffenceCodes("PC00000-001N"))).isTrue();

        assertThat(service.retainDueToOffence(offenderWithOffenceCodes("NOT A MATCH"))).isFalse();
        assertThat(service.retainDueToOffence(offenderWithNoOffenceCode())).isFalse();
    }

    @Test
    void retainDueToAlert() {
        assertThat(service.retainDueToAlert(offenderWithAlertCodes("C1"))).isTrue();
        assertThat(service.retainDueToAlert(offenderWithAlertCodes("XCSEA", "XTACT", "NOT A MATCH"))).isTrue();

        assertThat(service.retainDueToAlert(offenderWithAlertCodes("NOT A MATCH"))).isFalse();
        assertThat(service.retainDueToAlert(offenderWithAlertCodes())).isFalse();
    }

    private OffenderToCheck offenderWithOffenceCodes(final String... offenceCodes) {
        final var offender = OffenderToCheck.builder().offenderNumber(OFFENDER_NUMBER);
        stream(offenceCodes).forEach(offender::offenceCode);
        return offender.build();
    }

    private OffenderToCheck offenderWithAlertCodes(final String... alertCodes) {
        final var offender = OffenderToCheck.builder().offenderNumber(OFFENDER_NUMBER);
        stream(alertCodes).forEach(offender::alertCode);
        return offender.build();
    }

    private OffenderToCheck offenderWithNoOffenceCode() {
        return offenderWithOffenceCodes();
    }
}
