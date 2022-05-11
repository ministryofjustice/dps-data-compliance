package uk.gov.justice.hmpps.datacompliance.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffenderNumberTest {

    @Test
    void isValid() {

        assertThat(OffenderNumber.isValid("A1234AA")).isTrue();
        assertThat(OffenderNumber.isValid("X9999YZ")).isTrue();

        assertThat(OffenderNumber.isValid("A1234AAA")).isFalse();
        assertThat(OffenderNumber.isValid("AA1234AA")).isFalse();
        assertThat(OffenderNumber.isValid("AAAAAAA")).isFalse();
        assertThat(OffenderNumber.isValid("1234567")).isFalse();
        assertThat(OffenderNumber.isValid("INVALID")).isFalse();
        assertThat(OffenderNumber.isValid(",")).isFalse();
    }

    @Test
    void cannotCreateWithNullString() {

        assertThatThrownBy(() -> new OffenderNumber(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Null offender number");
    }

    @Test
    void cannotCreateWithInvalidId() {

        assertThatThrownBy(() -> new OffenderNumber("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid offender number: 'invalid'");
    }
}
