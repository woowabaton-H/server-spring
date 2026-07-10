package cleanloop.common.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /**
     * 카테고리 상태는 "지금" 기준으로 계산한다. 테스트에서 고정 시각으로 바꿀 수 있도록 빈으로 둔다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
