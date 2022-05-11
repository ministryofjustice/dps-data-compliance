package uk.gov.justice.hmpps.datacompliance.services.ual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck.OffenderToCheckBuilder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ual.OffenderUalEntity.OffenderUalEntityBuilder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.ual.OffenderUalRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UalServiceTest {

    public static final String CRO = "569151/08";
    public static final String PNC = "14/663516A";
    private static final String USERNAME = "user1";
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    @Mock
    OffenderUalRepository offenderUalRepository;

    private UalService ualService;

    @BeforeEach
    public void setUp() {
        ualService = new UalService(offenderUalRepository);
    }

    @Test
    void isUnlawfullyAtLarge_whenNoMatch() {
        var offenderToCheck = offenderToCheck().build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        offenderToCheck.getBookingNos().forEach(bookNo -> when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase(bookNo))
            .thenReturn(empty()));

        when(offenderUalRepository.findOneByOffenderPncIgnoreCase(PNC))
            .thenReturn(empty());

        when(offenderUalRepository.findOneByOffenderCroIgnoreCase(CRO))
            .thenReturn(empty());

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isFalse();
    }

    @Test
    void isUnlawfullyAtLarge_whenMatchOffenderNo() {
        var offenderToCheck = offenderToCheck().build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(Optional.of(offenderEntity().build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isTrue();
    }

    @Test
    void isUnlawfullyAtLarge_whenMatchBookNo() {
        var offenderToCheck = offenderToCheck().build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase("B07236"))
            .thenReturn(Optional.of(offenderEntity().build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isTrue();
    }

    @Test
    void isUnlawfullyAtLarge_whenMatchCro() {
        var offenderToCheck = offenderToCheck().build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        offenderToCheck.getBookingNos().forEach(bookNo -> when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase(bookNo))
            .thenReturn(empty()));

        when(offenderUalRepository.findOneByOffenderPncIgnoreCase(PNC))
            .thenReturn(empty());

        when(offenderUalRepository.findOneByOffenderCroIgnoreCase(CRO))
            .thenReturn(Optional.of(offenderEntity().build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isTrue();
    }

    @Test
    void isUnlawfullyAtLarge_whenFirstNameDoesNotMatch() {
        final var offenderToCheck_firstName = "abcdefghij";
        final var ualOffender_firstName = "abcdefghik";

        var offenderToCheck = offenderToCheck().firstName(offenderToCheck_firstName).build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        offenderToCheck.getBookingNos().forEach(bookNo -> when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase(bookNo))
            .thenReturn(empty()));

        when(offenderUalRepository.findOneByOffenderPncIgnoreCase(PNC))
            .thenReturn(Optional.of(offenderEntity().firstNames(ualOffender_firstName).build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isFalse();
    }

    @Test
    void isUnlawfullyAtLarge_whenFirstNameNotIdenticalButIsAboveThreshold() {
        final var offenderToCheck_firstName = "abcdefghi";
        final var offenderToCheck_middleName = "jklmnop";
        final var ualOffender_firstName = "abcdefghi jklmnoz";

        var offenderToCheck = offenderToCheck()
            .firstName(offenderToCheck_firstName)
            .middleName(offenderToCheck_middleName)
            .build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        offenderToCheck.getBookingNos().forEach(bookNo -> when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase(bookNo))
            .thenReturn(empty()));

        when(offenderUalRepository.findOneByOffenderPncIgnoreCase(PNC))
            .thenReturn(Optional.of(offenderEntity()
                .firstNames(ualOffender_firstName).build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isTrue();
    }


    @Test
    void isUnlawfullyAtLarge_whenLastNameDoesNotMatch() {
        final var offenderToCheck_lastName = "abcdefghij";
        final var ualOffender_lastName = "abcdefghik";

        var offenderToCheck = offenderToCheck().lastName(offenderToCheck_lastName).build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        offenderToCheck.getBookingNos().forEach(bookNo -> when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase(bookNo))
            .thenReturn(empty()));

        when(offenderUalRepository.findOneByOffenderPncIgnoreCase(PNC))
            .thenReturn(Optional.of(offenderEntity().lastName(ualOffender_lastName).build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isFalse();
    }

    @Test
    void isUnlawfullyAtLarge_whenLastNameNotIdenticalButIsAboveThreshold() {
        final var offenderToCheck_lastName = "abcdefghijklmnop";
        final var ualOffender_lastName = "abcdefghijklmnoz";

        var offenderToCheck = offenderToCheck()
            .lastName(offenderToCheck_lastName)
            .build();
        when(offenderUalRepository.findOneByOffenderNoIgnoreCase(offenderToCheck.getOffenderNumber().getOffenderNumber()))
            .thenReturn(empty());

        offenderToCheck.getBookingNos().forEach(bookNo -> when(offenderUalRepository.findOneByOffenderBookingNoIgnoreCase(bookNo))
            .thenReturn(empty()));

        when(offenderUalRepository.findOneByOffenderPncIgnoreCase(PNC))
            .thenReturn(Optional.of(offenderEntity()
                .lastName(ualOffender_lastName).build()));

        assertThat(ualService.isUnlawfullyAtLarge(offenderToCheck)).isTrue();
    }

    private OffenderUalEntityBuilder offenderEntity() {
        return OffenderUalEntity.builder()
            .offenderNo("A1234AA")
            .offenderBookingNo("AW3222")
            .offenderPnc(PNC)
            .offenderCro(CRO)
            .firstNames("Tom")
            .lastName("Smith")
            .offenceDescription("CONSPIRACY TO DEFRAUD")
            .userId(USERNAME)
            .uploadDateTime(NOW);

    }

    private OffenderToCheckBuilder offenderToCheck() {
        return OffenderToCheck.builder()
            .offenderNumber(new OffenderNumber("A1234AA"))
            .offenceCodes(Set.of("offenceCode"))
            .alertCodes(Set.of("alertCode"))
            .firstName("Tom")
            .lastName("Smith")
            .bookingNo("B07236")
            .bookingNo("Z07236")
            .pnc(PNC)
            .cro(CRO);
    }
}
