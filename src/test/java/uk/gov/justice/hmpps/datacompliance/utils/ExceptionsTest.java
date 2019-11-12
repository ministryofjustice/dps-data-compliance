package uk.gov.justice.hmpps.datacompliance.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.propagateAnyError;

class ExceptionsTest {

    @Test
    void propogateAnyError() {
        assertThatThrownBy(() -> propagateAnyError(ExceptionsTest::throwCheckedException))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("EXCEPTION!");
    }

    private static String throwCheckedException() throws IOException {
        throw new IOException("EXCEPTION!");
    }
}