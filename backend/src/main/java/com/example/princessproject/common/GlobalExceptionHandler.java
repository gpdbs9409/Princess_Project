package com.example.princessproject.common;

import com.example.princessproject.auth.service.AuthValidationException;
import com.example.princessproject.project.service.ProjectValidationException;
import com.example.princessproject.record.service.RecordValidationException;
import com.example.princessproject.upload.service.UploadValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onAuthValidation(AuthValidationException ex) {
        return new ApiErrorResponse(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ProjectValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onProjectValidation(ProjectValidationException ex) {
        return new ApiErrorResponse(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(RecordValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onRecordValidation(RecordValidationException ex) {
        return new ApiErrorResponse(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(UploadValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onUploadValidation(UploadValidationException ex) {
        return new ApiErrorResponse(ex.getCode(), ex.getMessage());
    }

    /**
     * Safety net for constraint violations that slip past application-level validation
     * (e.g. a race between two saves) - still structured instead of the raw Hibernate/SQL
     * message leaking to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new ApiErrorResponse("CONSTRAINT_VIOLATION", "Data integrity violation");
    }
}
