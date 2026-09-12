package com.example.princessproject.common;

import com.example.princessproject.auth.service.AuthValidationException;
import com.example.princessproject.admin.service.AdminValidationException;
import com.example.princessproject.commontask.service.CommonTaskValidationException;
import com.example.princessproject.project.service.ProjectValidationException;
import com.example.princessproject.record.service.RecordValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onInvalidRequest(MethodArgumentNotValidException ex) {
        boolean invalidEmail = ex.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> "email".equals(error.getField()));
        return invalidEmail
                ? new ApiErrorResponse("INVALID_EMAIL", "Invalid email format")
                : new ApiErrorResponse("INVALID_REQUEST", "Invalid request");
    }

    @ExceptionHandler(AuthValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onAuthValidation(AuthValidationException ex) {
        return new ApiErrorResponse(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AdminValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onAdminValidation(AdminValidationException ex) {
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

    @ExceptionHandler(CommonTaskValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse onCommonTaskValidation(CommonTaskValidationException ex) {
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

    /**
     * OpenAI 호출(비전 인증/레오집사 피드백)이 순간적으로 몰려 내부 대기열(OpenAiCallLimiter)에서도
     * 감당하지 못했을 때. 500이 아니라 503 + 명확한 코드로 내려줘서, 프론트가 "잠시 후 다시
     * 시도해주세요" 안내를 보여줄 수 있게 한다.
     */
    @ExceptionHandler(OpenAiCallLimiter.OpenAiOverloadedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse onOpenAiOverloaded(OpenAiCallLimiter.OpenAiOverloadedException ex) {
        return new ApiErrorResponse("OPENAI_OVERLOADED", ex.getMessage());
    }
}
