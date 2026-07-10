package cleanloop.common.response;

import cleanloop.common.request.RequestIdHolder;
import java.util.List;

/**
 * 성공 응답 공통 포맷: {"data": ..., "meta": {...}}
 */
public record ApiResponse<T>(T data, Meta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, new Meta(RequestIdHolder.get()));
    }

    /**
     * 커서 페이지네이션 목록 응답. 다음 페이지가 없으면 nextCursor는 null이다.
     */
    public static <T> ListApiResponse<T> list(List<T> data, String nextCursor) {
        return new ListApiResponse<>(data, new ListMeta(nextCursor, RequestIdHolder.get()));
    }

    public record ListApiResponse<T>(List<T> data, ListMeta meta) {
    }
}
