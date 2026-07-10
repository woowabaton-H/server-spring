package cleanloop.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;

/**
 * @EnableScheduling을 빠뜨려도 애플리케이션은 정상 기동한다.
 * 알림이 영영 생성되지 않는 상태를 컴파일이나 기동으로는 잡을 수 없어 등록 자체를 확인한다.
 */
@SpringBootTest
class NotificationSchedulerTest {

    @Autowired
    private ScheduledAnnotationBeanPostProcessor postProcessor;

    @Test
    void 알림_생성_스케줄러가_매일_오전_9시로_등록된다() {
        var cronTasks = postProcessor.getScheduledTasks().stream()
                .map(scheduledTask -> scheduledTask.getTask())
                .filter(CronTask.class::isInstance)
                .map(CronTask.class::cast)
                .toList();

        assertThat(cronTasks).hasSize(1);
        assertThat(cronTasks.get(0).getExpression()).isEqualTo("0 0 9 * * *");
    }
}
