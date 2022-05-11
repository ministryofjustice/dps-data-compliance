package uk.gov.justice.hmpps.datacompliance.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionsTest {

    private static String throwCheckedException() throws IOException {
        throw new IOException("EXCEPTION!");
    }

    @Test
    void propagateAnyError() {
        assertThatThrownBy(() -> Exceptions.propagateAnyError(ExceptionsTest::throwCheckedException))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("EXCEPTION!");
    }

    @Test
    void illegalState() {
        assertThat(Exceptions.illegalState("Some message with multiple variables: %s, %s", 1, "two").get())
            .hasMessage("Some message with multiple variables: 1, two");
    }
}