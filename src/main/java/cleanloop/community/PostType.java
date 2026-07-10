package cleanloop.community;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import java.util.Arrays;
import java.util.Map;

public enum PostType {

    TIPS("tips", "comments_count"),
    QA("qa", "answers_count");

    private final String code;
    private final String commentCounterColumn;

    PostType(String code, String commentCounterColumn) {
        this.code = code;
        this.commentCounterColumn = commentCounterColumn;
    }

    public String code() {
        return code;
    }

    /**
     * 같은 community_comments 행이라도 tips 글에서는 댓글로, qa 글에서는 답변으로 센다.
     * 컬럼명은 열거형이 소유하므로 외부 입력이 SQL에 섞이지 않는다.
     */
    public String commentCounterColumn() {
        return commentCounterColumn;
    }

    public static PostType from(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "type은 tips 또는 qa여야 합니다.", Map.of("type", String.valueOf(code))));
    }
}
