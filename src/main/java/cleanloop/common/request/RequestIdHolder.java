package cleanloop.common.request;

/**
 * 요청 단위 requestId 보관소. {@link RequestIdFilter}가 요청마다 채우고 비운다.
 */
public final class RequestIdHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    public static void set(String requestId) {
        HOLDER.set(requestId);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
