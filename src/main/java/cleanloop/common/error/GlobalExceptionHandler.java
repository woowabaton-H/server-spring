package cleanloop.common.error;

import cleanloop.common.response.ErrorResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        return toResponse(e.errorCode(), e.getMessage(), e.details());
    }

    /**
     * @Valid 바디 검증 실패. 필드별 사유를 details에 담는다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return toResponse(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), details);
    }

    /**
     * 아래 핸들러들은 예외 메시지를 그대로 내보내지 않는다.
     * Spring의 원문에는 내부 타입명과 스택 사정이 섞여 있어 클라이언트에 노출할 값이 아니다.
     * 대신 어떤 파라미터가 문제인지만 details에 구조화해 담는다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.debug("Missing request parameter", e);
        return toResponse(ErrorCode.INVALID_REQUEST, "필수 파라미터가 없습니다.",
                Map.of(e.getParameterName(), "필수 파라미터입니다."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("Request parameter type mismatch", e);
        return toResponse(ErrorCode.INVALID_REQUEST, "파라미터 형식이 올바르지 않습니다.",
                Map.of(e.getName(), "형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.debug("Unreadable request body", e);
        return toResponse(ErrorCode.INVALID_REQUEST, "요청 본문을 읽을 수 없습니다.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.debug("Method not supported", e);
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.defaultMessage(), null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        log.debug("Media type not supported", e);
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.defaultMessage(), null);
    }

    /**
     * 매핑되지 않은 경로처럼 Spring이 상태 코드를 이미 정한 예외는 그 상태를 보존한다.
     * NoResourceFoundException은 ErrorResponseException을 상속하지 않고
     * ErrorResponse 인터페이스만 구현하므로, 타입이 아니라 인터페이스로 판별해야
     * 404가 이 캐치올에서 500으로 뭉개지지 않는다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            if (status.is5xxServerError()) {
                log.error("Spring error response", e);
            }
            ErrorCode code = mapStatus(status);
            return ResponseEntity.status(status).body(ErrorResponse.of(code, code.defaultMessage(), null));
        }
        log.error("Unhandled exception", e);
        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null);
    }

    private ErrorCode mapStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_REQUEST;
        };
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode code, String message, Map<String, Object> details) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message, details));
    }
}
