package cleanloop.common.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    @Test
    void 인코딩한_커서는_원본으로_복원된다() {
        String raw = "2026-07-10T12:00:00|b0000000-0000-0000-0000-000000000001";

        assertThat(CursorCodec.decode(CursorCodec.encode(raw))).isEqualTo(raw);
    }

    @Test
    void 인코딩한_커서는_정렬키를_그대로_노출하지_않는다() {
        String raw = "2026-07-10T12:00:00";

        assertThat(CursorCodec.encode(raw)).doesNotContain(raw);
    }

    @Test
    void 잘못된_커서는_INVALID_CURSOR로_거부된다() {
        assertThatThrownBy(() -> CursorCodec.decode("!!not-base64!!"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).errorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR);
    }
}
