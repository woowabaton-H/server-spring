package cleanloop.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "잘못된 커서입니다."),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "요청 값 검증에 실패했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_DUPLICATED(HttpStatus.CONFLICT, "이미 같은 카테고리가 있습니다."),
    CATEGORY_PRESET_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리 프리셋을 찾을 수 없습니다."),

    SELECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "셀렉션을 찾을 수 없습니다."),
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "제공업체 옵션을 찾을 수 없습니다."),

    COMMUNITY_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "커뮤니티 글을 찾을 수 없습니다."),

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
