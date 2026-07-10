package cleanloop.global;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * backend-design 13의 주기 스케줄러를 활성화한다.
 * 인스턴스를 여러 대로 늘리면 중복 실행을 막을 잠금이 필요하다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
