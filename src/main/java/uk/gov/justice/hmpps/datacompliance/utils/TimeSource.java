package uk.gov.justice.hmpps.datacompliance.utils;

import java.time.*;

import static java.time.ZoneOffset.UTC;

public interface TimeSource {

    static TimeSource systemUtc() {
        Clock clock = Clock.systemUTC();
        return clock::instant;
    }

    static TimeSource of(final LocalDateTime dateTime) {
        return () -> dateTime.toInstant(UTC);
    }

    static TimeSource of(final LocalDate date) {
        return of(date.atStartOfDay());
    }

    Instant now();

    default LocalDateTime nowAsLocalDateTime() {
        return LocalDateTime.ofInstant(now(), UTC);
    }

    default LocalDate nowAsLocalDate() {
        return nowAsLocalDateTime().toLocalDate();
    }
}
