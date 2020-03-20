package uk.gov.justice.hmpps.datacompliance.web.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.justice.hmpps.datacompliance.web.dto.ErrorResponse;

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice(basePackageClasses = RetentionController.class)
@Slf4j
public class ControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(final EntityNotFoundException e) {
        return ResponseEntity
                .status(NOT_FOUND)
                .body(ErrorResponse
                        .builder()
                        .status(NOT_FOUND.value())
                        .userMessage("Entity Not Found")
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handle(final HttpClientErrorException e) {
        return ResponseEntity
                .status(e.getStatusCode())
                .body(ErrorResponse
                        .builder()
                        .status(e.getStatusCode().value())
                        .userMessage("Client Error")
                        .developerMessage(e.getStatusText())
                        .build());
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handle(final OptimisticLockException e) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(ErrorResponse
                        .builder()
                        .status(BAD_REQUEST.value())
                        .userMessage("Client Error")
                        .developerMessage(e.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(final Exception e) {

        log.error("Unexpected exception", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse
                        .builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .userMessage("Internal Server Error")
                        .developerMessage(e.getMessage())
                        .build());
    }
}
