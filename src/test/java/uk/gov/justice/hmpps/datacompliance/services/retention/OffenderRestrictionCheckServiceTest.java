package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderRestrictionCode;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.hmpps.datacompliance.services.retention.OffenderRestrictionCheckService.CHILD_RESTRICTION_COMMENT_REGEX;

@ExtendWith(MockitoExtension.class)
class OffenderRestrictionCheckServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final long RETENTION_CHECK_ID = 1;
    private static final int MAX_REGEX_LENGTH = 512;

    @Mock
    private DataComplianceEventPusher eventPusher;

    private OffenderRestrictionCheckService service;

    @BeforeEach
    void setUp() {
        service = new OffenderRestrictionCheckService(eventPusher);
    }

    @Test
    void requestOffenderRestrictionCheck() {

        service.requestOffenderRestrictionCheck(OFFENDER_NUMBER, RETENTION_CHECK_ID);

        verify(eventPusher).requestOffenderRestrictionCheck(
            OFFENDER_NUMBER,
            RETENTION_CHECK_ID,
            Set.of(OffenderRestrictionCode.CHILD),
            CHILD_RESTRICTION_COMMENT_REGEX);
    }

    @Test
    void regexLengthBelowMaximum() {
        assertThat(CHILD_RESTRICTION_COMMENT_REGEX.length()).isLessThan(MAX_REGEX_LENGTH);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"child", "teen", "teens", "young", "kid", "kids", "can't see his son", "restricted access to daughter", "under age", "minor", "minors", "youth"})
    void checkRegexMatch(String inputText) {
        assertThat(inputText.matches(CHILD_RESTRICTION_COMMENT_REGEX)).isTrue();
    }



}