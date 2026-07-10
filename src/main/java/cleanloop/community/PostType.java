package cleanloop.community;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import java.util.Arrays;
import java.util.Map;

public enum PostType {

    TIPS("tips"),
    QA("qa");

    private final String code;

    PostType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static PostType from(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "type은 tips 또는 qa여야 합니다.", Map.of("type", String.valueOf(code))));
    }
}
