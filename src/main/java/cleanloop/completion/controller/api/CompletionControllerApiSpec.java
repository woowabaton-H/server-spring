package cleanloop.completion.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.common.page.PageRequests;
import cleanloop.common.response.ApiResponse;
import cleanloop.completion.dto.CompleteCategoryRequest;
import cleanloop.completion.dto.CompleteCategoryResponse;
import cleanloop.completion.dto.CompletionLogResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Completion", description = "청소 카테고리 완료 및 기록 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface CompletionControllerApiSpec {

    @Operation(
            summary = "카테고리 완료 처리",
            description = """
                    홈 페이지의 완료 버튼을 클릭해 카테고리를 완료 처리한다.

                    **사용 페이지:**
                    - 홈: 오늘의 청소 완료 버튼 클릭

                    **플로우:**
                    1. 홈 → 카테고리 항목의 "완료" 버튼 클릭
                    2. POST /categories/{categoryId}/complete 호출
                    3. 완료 기록(completion_logs) 생성 + 카테고리 lastDoneAt 갱신 + 다음 관리 예정일 계산
                    4. 토스트 메시지 표시 (예: "욕실 완료. 다음 관리는 7월 24일")
                    5. 홈 상태 업데이트 → 완료한 카테고리의 상태 변경

                    이 요청은 트랜잭션으로 처리되어 완료 기록과 카테고리 상태가 함께 갱신된다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "카테고리 완료 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "완료 처리 응답",
                                    value = """
                                            {
                                              "data": {
                                                "category": {
                                                  "id": "cat_bath",
                                                  "name": "욕실",
                                                  "cycleDays": 14,
                                                  "lastDoneAt": "2026-07-10T12:00:00+09:00",
                                                  "nextDueAt": "2026-07-24T12:00:00+09:00",
                                                  "status": {
                                                    "code": "good",
                                                    "label": "14일 뒤 다시 보면 충분해요",
                                                    "daysUntilNext": 14
                                                  }
                                                },
                                                "log": {
                                                  "id": "log_10",
                                                  "categoryId": "cat_bath",
                                                  "categoryName": "욕실",
                                                  "completedAt": "2026-07-10T12:00:00+09:00"
                                                },
                                                "toastMessage": "욕실 완료. 다음 관리는 7월 24일에 보면 충분해요."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카테고리를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CompleteCategoryResponse> complete(
            @PathVariable UUID categoryId,
            @RequestBody(required = false) CompleteCategoryRequest request);

    @Operation(
            summary = "완료 기록 조회",
            description = """
                    사용자의 청소 완료 기록을 조회한다. 페이지네이션을 지원한다.

                    **사용 페이지:**
                    - 마이 > 완료 기록: 최근 완료 기록 목록 표시

                    **플로우:**
                    1. 마이 페이지 로드
                    2. GET /completion-logs 호출 (선택: from, to 필터링, cursor 기반 페이지네이션)
                    3. 완료 기록 목록 수신
                    4. 주간/월간 집계 데이터로 변환해 UI에 표시

                    페이지네이션:
                    - cursor 기반 페이지네이션 지원 (limit 기본값: 20, 최대: 100)
                    - from/to로 날짜 범위 필터링 가능

                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "완료 기록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "완료 기록 목록",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": "log_10",
                                                  "categoryId": "cat_bath",
                                                  "categoryName": "욕실",
                                                  "completedAt": "2026-07-10T12:00:00+09:00"
                                                }
                                              ],
                                              "meta": {
                                                "nextCursor": null
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
    ApiResponse.ListApiResponse<CompletionLogResponse> findLogs(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);
}
