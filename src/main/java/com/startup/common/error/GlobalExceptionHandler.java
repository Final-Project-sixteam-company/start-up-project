package com.startup.common.error;

import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestControllerAdvice
// 컨트롤러 밖으로 올라온 예외를 ApiResponse.fail(...) 형식으로 통일한다.
// 예상 가능한 비즈니스/검증 오류는 warn, 알 수 없는 서버 오류는 error로 남긴다.
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request
    ) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(errorCode, e.getMessage(), request.getRequestURI(), e.getDetails())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        return badRequest(firstFieldErrorMessage(e.getBindingResult(), CommonErrorCode.INVALID_INPUT_VALUE.getMessage()), request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("BindException: {}", e.getMessage());
        return badRequest(firstFieldErrorMessage(e.getBindingResult(), CommonErrorCode.INVALID_INPUT_VALUE.getMessage()), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(CommonErrorCode.INVALID_INPUT_VALUE.getMessage());

        return badRequest(message, request);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e, HttpServletRequest request) {
        log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
        return badRequest(e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        // 본문 역직렬화 실패(잘못된 JSON, 타입 불일치, 잘못된 enum 값 등)는 클라이언트 입력 오류이므로 400으로 매핑한다.
        // 원본 예외 메시지에는 입력값/내부 타입 정보가 섞일 수 있어 노출하지 않고 고정 메시지를 사용한다.
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return badRequest("요청 본문을 읽을 수 없습니다. 요청 형식을 확인해 주세요.", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        log.warn("MaxUploadSizeExceededException: {}", e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.PAYLOAD_TOO_LARGE.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(
                        CommonErrorCode.PAYLOAD_TOO_LARGE,
                        CommonErrorCode.PAYLOAD_TOO_LARGE.getMessage(),
                        request.getRequestURI()
                )));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e, HttpServletRequest request) {
        log.warn("MultipartException: {}", e.getMessage());
        return badRequest("Invalid multipart request.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        log.debug("NoResourceFoundException: {}", e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.NOT_FOUND.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(
                        CommonErrorCode.NOT_FOUND,
                        CommonErrorCode.NOT_FOUND.getMessage(),
                        request.getRequestURI()
                )));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(
                        CommonErrorCode.METHOD_NOT_ALLOWED,
                        e.getMessage(),
                        request.getRequestURI()
                )));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("HttpMediaTypeNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus())
                .body(ApiResponse.fail(buildErrorResponse(
                        CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
                        e.getMessage(),
                        request.getRequestURI()
                )));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(buildErrorResponse(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        request.getRequestURI()
                )));
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message, HttpServletRequest request) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(buildErrorResponse(
                        CommonErrorCode.INVALID_INPUT_VALUE,
                        message,
                        request.getRequestURI()
                )));
    }

    private String firstFieldErrorMessage(org.springframework.validation.BindingResult bindingResult, String defaultMessage) {
        // Bean Validation 오류가 여러 개여도 클라이언트에는 가장 먼저 잡힌 필드 메시지만 내려준다.
        if (bindingResult.getFieldError() == null || bindingResult.getFieldError().getDefaultMessage() == null) {
            return defaultMessage;
        }
        return bindingResult.getFieldError().getDefaultMessage();
    }

    private ErrorResponse buildErrorResponse(ErrorCode errorCode, String message, String path) {
        return buildErrorResponse(errorCode, message, path, null);
    }

    private ErrorResponse buildErrorResponse(ErrorCode errorCode, String message, String path, Map<String, Object> details) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(message)
                .path(path)
                .details(details)
                .build();
    }
}
