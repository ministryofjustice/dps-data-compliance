package uk.gov.justice.hmpps.datacompliance.utils;

import lombok.AllArgsConstructor;

import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

@AllArgsConstructor(access = PRIVATE)
public class Result<SUCCESS, ERROR> {

    private final SUCCESS success;
    private final ERROR error;

    public static <SUCCESS, ERROR> Result<SUCCESS, ERROR> success(final SUCCESS value) {
        return new Result<>(requireNonNull(value), null);
    }

    public static <SUCCESS, ERROR> Result<SUCCESS, ERROR> error(final ERROR value) {
        return new Result<>(null, requireNonNull(value));
    }

    public SUCCESS get() {
        checkState(isSuccess(), "No success value present, error value is: '%s'", error);
        return success;
    }

    public ERROR getError() {
        checkState(isError(), "No error value present, success value is: '%s'", success);
        return error;
    }

    public boolean isSuccess() {
        return success != null;
    }

    public boolean isError() {
        return error != null;
    }

    public void ifSuccess(final Consumer<SUCCESS> onSuccess) {
        if (isSuccess()) {
            onSuccess.accept(success);
        }
    }

    public void handleError(final Consumer<ERROR> errorHandler) {
        checkState(isError(), "No error value present, success value is: '%s'", success);
        errorHandler.accept(error);
    }
}
