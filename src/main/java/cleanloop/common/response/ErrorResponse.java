package cleanloop.common.response;

import cleanloop.common.error.ErrorCode;
import cleanloop.common.request.RequestIdHolder;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * 에러 응답 공통 포맷: {"error": {"code", "message", "details"}, "meta": {...}}
 */
public record ErrorResponse(Body error, Meta meta) {

    public static ErrorResponse of(ErrorCode code, String message, Map<String, Object> details) {
        return new ErrorResponse(new Body(code.name(), message, details), new Meta(RequestIdHolder.get()));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Body(String code, String message, Map<String, Object> details) {
    }
}
