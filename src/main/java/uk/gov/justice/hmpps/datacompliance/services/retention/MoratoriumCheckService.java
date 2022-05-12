package uk.gov.justice.hmpps.datacompliance.services.retention;

import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class MoratoriumCheckService {

    public static final String RED_FLAGS = ".*(groom|nonce|paedo|pedo|peedo|paeda|peeda|pedaph|pedaf).*";
    private static final String LINKS_TO_CHILDREN = "(adopt|baby|boy([^f]|$)|child|chidl|chlid|chid|cild|hild|daughter|early age|girl([^f]|$)|infant|juven|kid|minor(\\W|$)|niece|nephew|(^|\\W)(grand)?son(\\W|$)|(^|\\W)teen|toddler|under( (the )?)?age|youth|young)";
    private static final String LINKS_TO_PUBLIC_ROLES = "(borstal|college|faith|foster|nurser|public app|religi|school([^ie]|$)|teach|uniformed|voluntary|yoi)";
    private static final String LINKS_TO_ABUSE_OR_RISK = "(abduct|abuse|bugger|danger|expose|exploit|explicit|inappropriate|indecent|internet|intercourse|kidnap|molest|neglect|online|penatrat|porn|predator|prostitut|protect|rape|risk|safe|sex|sodom|s(h)?(o)?po|(^.{0,3}|([^i][^n].)|[^i]n.)touch)";
    public static final String PUBLIC_ROLE_ABUSE_REGEX = ".*" + LINKS_TO_PUBLIC_ROLES + ".*" + LINKS_TO_ABUSE_OR_RISK + ".*";
    public static final String PUBLIC_ROLE_ABUSE_REGEX_REVERSED = ".*" + LINKS_TO_ABUSE_OR_RISK + ".*" + LINKS_TO_PUBLIC_ROLES + ".*";
    public static final String CHILD_ABUSE_REGEX = ".*" + LINKS_TO_CHILDREN + ".*" + LINKS_TO_ABUSE_OR_RISK + ".*";
    public static final String CHILD_ABUSE_REGEX_REVERSED = ".*" + LINKS_TO_ABUSE_OR_RISK + ".*" + LINKS_TO_CHILDREN + ".*";

    // TODO GDPR-153 Persist offence codes to the DB:
    private static final Set<String> CHILD_ABUSE_LINKED_OFFENCE_CODES =
        Set.of("SX03001-004N", "SX03035-038N", "SX03017-018N", "SX03007-008N", "SX03070-077N", "SX03114-115N",
            "SX03054-057N", "SX03062-175N", "SX03116-117N", "SX03102-111N", "SX03058-059N", "SX03112-113N",
            "SX03060-200N", "SX03050-192N", "SX03048-049N", "SX03031-044N", "SX03019-022N", "MH59008-010N",
            "SX03045-046N", "SX03027-042N", "SX56051-052NA", "SX56014-074N", "SX03015-016N", "PK78001-008NA",
            "SX03013-014N", "SX56087-088N", "SX56025-029N", "SX56070-072N", "SX56047-050N", "SX56004N",
            "SX56007N", "SX56008N", "SX56031N", "SX56032N", "SX56037N", "SX56038N", "SX56042N", "SX56043N",
            "SX56046N", "SX56053N", "SX67001N", "SX67002N", "TH68031N", "XX0109", "SX03164-188N", "SA00001",
            "SA00002", "SX56005-006N", "SX56017", "SX56070", "SX56111", "SX56076", "SX56101", "SX56026",
            "SX56112", "SX56072", "SX56121", "SX56005", "SX56006", "SX56079", "SX56119", "SX56081", "SX56085",
            "SX56014", "SX56088", "SX56044", "SX56083", "SX56120", "SX56075", "SX56099", "SX56104", "SX56035",
            "SX56102", "SX56017A", "SX56118", "SX56078", "SX56122", "SX56049", "SX56018", "SX56042", "SX56106",
            "SX56077", "SX56018A", "SX56089", "SX56025", "SX56010", "SX56115", "SX56028", "SX56084", "SX56014A",
            "SX56087", "SX56045", "SX56047", "SX56074", "SX56015", "SX56113", "SX56116", "SX56048", "SX56030",
            "SX56114", "SX56082", "SX56029", "SX56103", "SX56036", "SX56015A", "SX56027", "SX56100", "SX56050",
            "SX56105", "SX56026A", "SX56026B", "SX56029A", "SX56109", "SX56110", "SX56070A", "SX56071A",
            "SX56072A", "SX56073A", "SX56073", "FL96001", "FL96001A", "FL96002", "PK78001", "PK78002",
            "PK78003", "PK78004", "PK78005", "PK78006", "PK78007", "PK78008", "PK78003A", "IC60003", "IC60004",
            "IC60005", "IC60006", "IC60007", "IC60008", "SO96009", "SO96010", "SO96012", "SO96013", "SO96014",
            "SX03230", "SX03113A", "SX03232", "SX03230A", "SX03031", "SX03032", "SX03033", "SX03034", "SX03035",
            "SX03036", "SX03037", "SX03038", "SX03039", "SX03040", "SX03041", "SX03042", "SX03043", "SX03044",
            "SX03045", "SX03046", "SX03047", "SX03048", "SX03049", "SX03050", "SX03051", "SX03052", "SX03053",
            "SX03054", "SX03055", "SX03056", "SX03057", "SX03058", "SX03059", "SX03060", "SX03061", "SX03062",
            "SX03063", "SX03064", "SX03065", "SX03066", "SX03067", "SX03068", "SX03069", "SX03070", "SX03071",
            "SX03072", "SX03073", "SX03074", "SX03075", "SX03076", "SX03077", "SX03102", "SX03103", "SX03104",
            "SX03105", "SX03106", "SX03107", "SX03108", "SX03109", "SX03110", "SX03111", "SX03112", "SX03113",
            "SX03114", "SX03115", "SX03116", "SX03117", "SX03126", "SX03127", "SX03156", "SX03157", "SX03158",
            "SX03159", "SX03160", "SX03161", "SX03162", "SX03163", "SX03164", "SX03165", "SX03166", "SX03167",
            "SX03168", "SX03169", "SX03170", "SX03171", "SX03172", "SX03173", "SX03174", "SX03175", "SX03176",
            "SX03177", "SX03178", "SX03179", "SX03181", "SX03182", "SX03183", "SX03184", "SX03185", "SX03186",
            "SX03187", "SX03188", "SX03189", "SX03190", "SX03191", "SX03192", "SX03193", "SX03194", "SX03195",
            "SX03196", "SX03197", "SX03198", "SX03199", "SX03200", "SX03001", "SX03001A", "SX03002", "SX03002A",
            "SX03003", "SX03003A", "SX03004", "SX03004A", "SX03005", "SX03006", "SX03009", "SX03010", "SX03011",
            "SX03012", "SX03013", "SX03013A", "SX03014", "SX03014A", "SX03015", "SX03016", "SX03017", "SX03018",
            "SX03019", "SX03020", "SX03021", "SX03022", "SX03023", "SX03024", "SX03025", "SX03026", "SX03027",
            "SX03028", "SX03029", "SX03030", "SX03210", "SX03211", "SX03001B", "SX03001C", "SX03002C",
            "SX03006B", "SX03013B", "SX03014B", "SX03015A", "SX03017B", "SX03018B", "SX03019C", "SX03020C",
            "SX03021A", "SX03023B", "SX03025C", "SX03027A", "SX03028A", "SX03029A", "SX03030A", "SX03031B",
            "SX03032A", "SX03033A", "SX03034A", "SX03047A", "SX03062C", "SX03063C", "SX03064C", "SX03065C",
            "SX03066C", "SX03067C", "SX03068C", "SX03069C", "SX03106A", "SX03116C", "SX03117C", "SX03157B",
            "SX03158A", "SX03162A", "SX03170A", "SX03172C", "SX03173C", "SX03174C", "SX03175C", "SX03181C",
            "SX03182C", "SX03183C", "SX03184C", "SX03210A", "SX03211A", "SX03229", "SX03010A", "SX03009A",
            "SX03008C", "SX03007C", "SX03224A", "SX03003C", "SX03004C", "SX03005A", "SX03005C", "SX03006A",
            "SX03006C", "SX03009C", "SX03010C", "SX03011C", "SX03012A", "SX03012C", "SX03013C", "SX03014C",
            "SX03015C", "SX03016A", "SX03016C", "SX03017A", "SX03017C", "SX03018A", "SX03018C", "SX03019A",
            "SX03020A", "SX03021C", "SX03022A", "SX03022C", "SX03023A", "SX03023C", "SX03024A", "SX03024C",
            "SX03025A", "SX03026A", "SX03026C", "SX03027C", "SX03028C", "SX03029C", "SX03030C", "SX03031A",
            "SX03035A", "SX03036A", "SX03037A", "SX03038A", "SX03039A", "SX03040A", "SX03041A", "SX03042A",
            "SX03043A", "SX03044A", "SX03161A", "SX03224", "SX03225", "SX03225A", "SX03226", "SX03226A",
            "SX03227", "SX03227C", "SX03228", "SX03228C", "SX03231", "CJ09001", "SC15003", "PH17001");

    private static final Set<String> SECTION_40_CONVICTION_OFFENCE_CODES = Set.of("PC00000-001N");

    private static final Set<String> OFFENCE_CODES_CAUSING_RETENTION = ImmutableSet.<String>builder()
        .addAll(CHILD_ABUSE_LINKED_OFFENCE_CODES)
        .addAll(SECTION_40_CONVICTION_OFFENCE_CODES)
        .build();

    // TODO GDPR-197 Persist alert codes to DB
    private static final Set<String> ALERT_CODES_CAUSING_RETENTION = Set.of(
        "C1", "C2", "C3", "C4", "CC1", "CC2", "CC3", "CC4", "CPC", "CPRC", "HPI", "OCYP", "P0", "P1", "P2", "P3",
        "PC1", "PC2", "PC3", "PL1", "PL2", "PL3", "PVN", "RCC", "RCP", "RCS", "RDO", "RVR", "RYP", "SC", "XCC",
        "XCSEA", "XTACT");

    private final DataComplianceEventPusher eventPusher;

    public void requestFreeTextSearch(final OffenderNumber offenderNumber, final Long retentionCheckId) {

        log.debug("Submitting a request to perform free text search for offender: '{}/{}'",
            offenderNumber.getOffenderNumber(), retentionCheckId);

        eventPusher.requestFreeTextMoratoriumCheck(
            offenderNumber,
            retentionCheckId,
            List.of(
                RED_FLAGS,
                CHILD_ABUSE_REGEX,
                CHILD_ABUSE_REGEX_REVERSED,
                PUBLIC_ROLE_ABUSE_REGEX,
                PUBLIC_ROLE_ABUSE_REGEX_REVERSED));
    }

    public boolean retainDueToOffence(final OffenderToCheck offenderToCheck) {

        log.debug("Checking if offender '{}' must be retained due to their offences",
            offenderToCheck.getOffenderNumber().getOffenderNumber());

        return offenderToCheck.getOffenceCodes().stream()
            .anyMatch(OFFENCE_CODES_CAUSING_RETENTION::contains);
    }

    public boolean retainDueToAlert(final OffenderToCheck offenderToCheck) {

        log.debug("Checking if offender '{}' must be retained due to alerts",
            offenderToCheck.getOffenderNumber().getOffenderNumber());

        return offenderToCheck.getAlertCodes().stream()
            .anyMatch(ALERT_CODES_CAUSING_RETENTION::contains);
    }
}
