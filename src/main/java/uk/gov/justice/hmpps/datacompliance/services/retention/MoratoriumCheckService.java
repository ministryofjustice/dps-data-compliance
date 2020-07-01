package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import javax.transaction.Transactional;
import java.util.List;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MoratoriumCheckService {

    private static final String LINKS_TO_CHILDREN_OR_PUBLIC_ROLES = "(" +
            "adopt|" +
            "baby|" +
            "borstal|" +
            "boy|" +
            "child|" +
            "chidl|" +
            "chlid|" +
            "chid|" +
            "cild|" +
            "hild|" +
            "college|" +
            "daughter|" +
            "early age|" +
            "faith|" +
            "foster|" +
            "girl|" +
            "grandson" +
            "infant|" +
            "juven|" +
            "kid|" +
            "minor|" +
            "niece|" +
            "nephew|" +
            "nurser|" +
            "public|" +
            "religi|" +
            "school|" +
            "[^a-z]*son[^a-z]*|" +
            "teach|" +
            "teen|" +
            "toddler|" +
            "underage|" +
            "under age|" +
            "under the age|" +
            "uniformed|" +
            "voluntary|" +
            "youth|" +
            "yoi|" +
            "young" +
            ")";

    private static final String LINKS_TO_ABUSE_OR_RISK = "(" +
            "abduct|" +
            "abuse|" +
            "bugger|" +
            "expose|" +
            "exploit|" +
            "explicit|" +
            "groom|" +
            "inappropriate|" +
            "indecent|" +
            "internet|" +
            "molest|" +
            "neglect|" +
            "online|" +
            "porn|" +
            "predator|" +
            "protect|" +
            "rape|" +
            "risk|" +
            "safe|" +
            "sex|" +
            "sodom|" +
            "sopo|" +
            "shpo|" +
            "shopo|" +
            "touch" +
            ")";

    public static final String RED_FLAGS = ".*(nonce|paedo|pedo|peedo|paeda|peeda|pedaph|pedaf).*";
    public static final String CHILD_ABUSE_REGEX = ".*" + LINKS_TO_CHILDREN_OR_PUBLIC_ROLES + ".*" + LINKS_TO_ABUSE_OR_RISK + ".*";
    public static final String CHILD_ABUSE_REGEX_REVERSED = ".*" + LINKS_TO_ABUSE_OR_RISK + ".*" + LINKS_TO_CHILDREN_OR_PUBLIC_ROLES + ".*";

    private final DataComplianceEventPusher eventPusher;

    public void requestFreeTextSearch(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to perform free text search for offender: '{}/{}'",
                offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestFreeTextMoratoriumCheck(
                offenderNumber,
                retentionCheckId,
                List.of(RED_FLAGS, CHILD_ABUSE_REGEX, CHILD_ABUSE_REGEX_REVERSED));
    }
}
