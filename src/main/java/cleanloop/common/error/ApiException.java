package cleanloop.common.error;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, Object> details;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Map<String, Object> details() {
        return details;
    }
}
