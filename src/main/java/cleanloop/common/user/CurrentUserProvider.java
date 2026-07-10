package cleanloop.common.user;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MVP에는 인증이 없다. 모든 요청은 설정에 고정된 데모 사용자 컨텍스트로 동작한다.
 * 정식 로그인이 붙으면 이 구현만 요청 컨텍스트 기반으로 교체한다.
 */
@Component
public class CurrentUserProvider {

    private final UUID demoUserId;

    public CurrentUserProvider(@Value("${cleanloop.demo-user-id}") String demoUserId) {
        this.demoUserId = UUID.fromString(demoUserId);
    }

    public UUID currentUserId() {
        return demoUserId;
    }
}
