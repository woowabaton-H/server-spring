package cleanloop.common.page;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;

/**
 * limit 파라미터 정규화. MVP 전 목록 API가 같은 규칙을 쓴다.
 */
public final class PageRequests {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private PageRequests() {
    }

    public static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "limit은 1 이상 %d 이하여야 합니다.".formatted(MAX_LIMIT));
        }
        return limit;
    }
}
