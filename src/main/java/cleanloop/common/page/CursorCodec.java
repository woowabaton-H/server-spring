package cleanloop.common.page;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 커서 페이지네이션용 불투명 커서 인코딩.
 * 정렬 키를 그대로 노출하지 않기 위해 base64로 감싼다.
 */
public final class CursorCodec {

    private CursorCodec() {
    }

    public static String encode(String rawKey) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String cursor) {
        try {
            return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
    }
}
