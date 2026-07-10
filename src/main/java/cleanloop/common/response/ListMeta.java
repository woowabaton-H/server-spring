package cleanloop.common.response;

/**
 * 목록 응답의 meta. 다음 페이지가 없을 때도 nextCursor를 null로 명시해 내려준다.
 */
public record ListMeta(String nextCursor, String requestId) {
}
