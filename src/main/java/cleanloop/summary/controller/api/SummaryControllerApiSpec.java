package cleanloop.summary.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.common.response.ApiResponse;
import cleanloop.summary.dto.HomeResponse;
import cleanloop.summary.dto.MeSummaryResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Summary", description = "홈 요약 및 마이 페이지 요약 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface SummaryControllerApiSpec {

    @Operation(
            summary = "홈 요약 조회",
            description = """
                    홈 페이지에 표시할 오늘의 청소 현황을 조회한다.

                    **사용 페이지:**
                    - 홈 페이지: 메인 화면, 앱 진입 시 첫 로드

                    **플로우:**
                    1. 앱 진입 → 홈 페이지 로드
                    2. GET /home (선택: today 파라미터로 특정 날짜 지정)
                    3. 오늘(또는 지정 날짜)의 청소 현황 수신
                    4. 활성 카테고리 목록, 상태, 최근 완료 기록 렌더링

                    반환 정보:
                    - today: 기준일 (사용자 타임존 기준)
                    - message: 동기 부여 메시지
                    - monthlyCompletionCount: 현재 월 완료 수
                    - categories: 활성 카테고리 + 상태 (due/soon/good)
                    - recentLogs: 최근 완료 기록
                    - unreadNotificationCount: 읽지 않은 알림 수

                    처리 규칙:
                    - categories는 활성 카테고리만 반환
                    - nextDueAt과 status는 서버에서 계산
                    - monthlyCompletionCount는 사용자 타임존 기준 현재 월
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "홈 요약 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "홈 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "today": "2026-07-10",
                                                "message": "오늘 청소부터 챙겨요.",
                                                "monthlyCompletionCount": 5,
                                                "categories": [
                                                  {
                                                    "id": "cat_bath",
                                                    "name": "욕실",
                                                    "icon": "bath",
                                                    "cycleDays": 14,
                                                    "lastDoneAt": "2026-07-01T09:00:00+09:00",
                                                    "nextDueAt": "2026-07-15T09:00:00+09:00",
                                                    "note": "물때와 습기만 잡아도 관리가 쉬워져요.",
                                                    "status": {
                                                      "code": "good",
                                                      "label": "5일 뒤 다시 보면 충분해요",
                                                      "daysUntilNext": 5
                                                    }
                                                  }
                                                ],
                                                "recentLogs": [
                                                  {
                                                    "id": "log_01",
                                                    "categoryId": "cat_trash",
                                                    "categoryName": "쓰레기/수거",
                                                    "completedAt": "2026-07-08T21:00:00+09:00"
                                                  }
                                                ],
                                                "unreadNotificationCount": 1
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<HomeResponse> home(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today);

    @Operation(
            summary = "마이 페이지 요약 조회",
            description = """
                    마이 페이지에 표시할 사용자 활동 요약을 조회한다.

                    **사용 페이지:**
                    - 마이 페이지: 사용자 프로필, 활동 통계, 최근 기록 표시

                    **플로우:**
                    1. 마이 탭 진입
                    2. GET /me/summary 호출
                    3. 사용자 프로필, 통계, 주간 활동, 저장한 셀렉션 수신
                    4. 마이 페이지 렌더링

                    반환 정보:
                    - profile: 사용자 정보 (이름, 아바타)
                    - stats: 월간 완료 수, 카테고리 수, 저장한 셀렉션 수
                    - weeklyFootprints: 최근 12주간의 주간 완료 수 (깃허브 스트릭처럼 시각화용)
                    - recentLogs: 최근 완료 기록 3-5개
                    - savedSelections: 저장한 셀렉션 미리보기

                    처리 규칙:
                    - weeklyFootprints는 최근 12주 반환
                    - level은 주간 완료 수를 0, 1, 2, 3 등으로 표현 (히트맵용)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "마이 요약 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "마이 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "profile": {
                                                  "id": "usr_01",
                                                  "name": "김보송",
                                                  "avatarText": "보"
                                                },
                                                "stats": {
                                                  "monthlyCompletionCount": 5,
                                                  "categoryCount": 6,
                                                  "savedSelectionCount": 1
                                                },
                                                "weeklyFootprints": [
                                                  {
                                                    "weekStartDate": "2026-04-20",
                                                    "completionCount": 1,
                                                    "level": 1
                                                  },
                                                  {
                                                    "weekStartDate": "2026-07-06",
                                                    "completionCount": 2,
                                                    "level": 2
                                                  }
                                                ],
                                                "recentLogs": [
                                                  {
                                                    "id": "log_10",
                                                    "categoryId": "cat_bath",
                                                    "categoryName": "욕실",
                                                    "completedAt": "2026-07-10T12:00:00+09:00"
                                                  }
                                                ],
                                                "savedSelections": [
                                                  {
                                                    "id": "starter-kit",
                                                    "title": "자취 첫 달 기본 청소 키트",
                                                    "category": "전체",
                                                    "type": "kit",
                                                    "priceText": "19,000원대",
                                                    "isSaved": true
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<MeSummaryResponse> meSummary();
}
