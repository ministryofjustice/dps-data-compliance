package uk.gov.justice.hmpps.datacompliance.utils;

import org.junit.jupiter.api.Test;

import static java.time.LocalDate.EPOCH;
import static org.assertj.core.api.Assertions.assertThat;

class TimeSourceTest {

    @Test
    void timeSourceCanBeSet() {

        var source = TimeSource.of(EPOCH);

        assertThat(source.now().toEpochMilli()).isZero();
        assertThat(source.nowAsLocalDate()).isNotNull();
     //   assertThat(source.nowAsLocalDateTime()).isEqualTo(EPOCH.atStartOfDay());
    }
}