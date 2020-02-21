package uk.gov.justice.hmpps.datacompliance.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.error;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.success;

@ExtendWith(MockitoExtension.class)
class ResultTest {

    @Test
    void resultSuccess() {

        final var result = success(1);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isError()).isFalse();
        assertThat(result.get()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ifSuccess() {

        final var result = success(1);
        final var consumer = mock(Consumer.class);

        result.ifSuccess(consumer);

        verify(consumer).accept(1);
    }

    @Test
    void getErrorThrowsIfResultIsSuccess() {

        final var result = success(1);

        assertThatThrownBy(result::getError)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No error value present, success value is: '1'");
    }

    @Test
    void handleErrorThrowsIfResultIsSuccess() {

        final var result = success(1);

        assertThatThrownBy(() -> result.handleError(error -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No error value present, success value is: '1'");
    }

    @Test
    void resultError() {

        final var result = error(1);

        assertThat(result.isError()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleError() {

        final var result = error(1);
        final var consumer = mock(Consumer.class);

        result.handleError(consumer);

        verify(consumer).accept(1);
    }

    @Test
    void getThrowsIfResultIsError() {

        final var result = error(1);

        assertThatThrownBy(result::get)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No success value present, error value is: '1'");
    }

    @Test
    @SuppressWarnings("unchecked")
    void ifSuccessDoesNotCallConsumerIfResultIsError() {

        final var result = error(1);
        final var consumer = mock(Consumer.class);

        result.ifSuccess(consumer);

        verifyNoInteractions(consumer);
    }
}