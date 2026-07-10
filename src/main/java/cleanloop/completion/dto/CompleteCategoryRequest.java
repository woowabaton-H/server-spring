package cleanloop.completion.dto;

import java.time.OffsetDateTime;

/**
 * completedAt을 생략하면 서버의 현재 시각으로 완료 처리한다.
 */
public record CompleteCategoryRequest(OffsetDateTime completedAt) {
}
