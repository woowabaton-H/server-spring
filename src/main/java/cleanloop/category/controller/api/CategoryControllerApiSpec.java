package cleanloop.category.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.category.dto.CategoryResponse;
import cleanloop.category.dto.CreateCategoryRequest;
import cleanloop.category.dto.UpdateCategoryRequest;
import cleanloop.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Category", description = "청소 카테고리 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface CategoryControllerApiSpec {

    @Operation(
            summary = "활성 카테고리 목록 조회",
            description = """
                    홈 페이지와 마이 페이지에서 사용자가 관리 중인 활성 카테고리 목록을 조회한다.

                    **사용 페이지:**
                    - 홈: 오늘의 청소 체크리스트 표시 시
                    - 마이: 관리 중인 카테고리 요약 표시 시

                    **플로우:**
                    1. 앱 진입 → 홈 로드
                    2. GET /categories 호출 → 활성 카테고리 목록 수신
                    3. 각 카테고리의 상태(due/soon/good)를 계산해 UI에 렌더링

                    카테고리는 sort_order 기준으로 정렬되며, 각 항목에는 다음 관리 예정일과 현재 상태가 포함된다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "카테고리 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(
                                    name = "성공 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": "cat_kitchen",
                                                  "name": "주방",
                                                  "icon": "kitchen",
                                                  "cycleDays": 7,
                                                  "lastDoneAt": "2026-07-06T20:00:00+09:00",
                                                  "nextDueAt": "2026-07-13T20:00:00+09:00",
                                                  "note": "배수구와 조리대 표면을 기준으로 잡아요.",
                                                  "status": {
                                                    "code": "good",
                                                    "label": "3일 뒤 다시 보면 충분해요",
                                                    "daysUntilNext": 3
                                                  },
                                                  "createdAt": "2026-07-10T09:00:00+09:00",
                                                  "updatedAt": "2026-07-10T09:00:00+09:00"
                                                }
                                              ]
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
    ApiResponse<List<CategoryResponse>> findAll();

    @Operation(
            summary = "프리셋 기반 또는 직접 입력으로 새 카테고리 생성",
            description = """
                    카테고리 추가 페이지에서 프리셋을 선택하거나 직접 입력해 새 카테고리를 생성한다.

                    **사용 페이지:**
                    - 카테고리 추가 페이지: "카테고리 추가" 버튼 클릭 후

                    **플로우:**
                    1. 홈/마이 → "카테고리 추가" 버튼 클릭
                    2. 카테고리 추가 페이지 로드
                    3. 프리셋 선택 또는 직접 입력
                    4. POST /categories 호출 (presetKey 또는 name/icon/cycleDays/note 전송)
                    5. 새 카테고리 생성 → 목록에 즉시 추가

                    중복 방지:
                    - 같은 프리셋이 이미 존재하면 생성 불가
                    - 같은 이름의 활성 카테고리가 이미 존재하면 생성 불가
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "카테고리 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "생성된 카테고리",
                                    value = """
                                            {
                                              "data": {
                                                "id": "cat_pet",
                                                "name": "반려동물",
                                                "icon": "floor",
                                                "cycleDays": 7,
                                                "lastDoneAt": "2026-07-10T12:00:00+09:00",
                                                "nextDueAt": "2026-07-17T12:00:00+09:00",
                                                "note": "털, 냄새, 패드 주변을 한 카테고리로 관리해요.",
                                                "status": {
                                                  "code": "good",
                                                  "label": "7일 뒤 다시 보면 충분해요",
                                                  "daysUntilNext": 7
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (cycleDays가 유효하지 않음 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복 또는 상태 충돌 (같은 카테고리가 이미 존재)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request);

    @Operation(
            summary = "카테고리 주기, 이름, 설명 수정",
            description = """
                    카테고리 편집 페이지에서 주기, 이름, 설명을 수정한다.

                    **사용 페이지:**
                    - 카테고리 편집 페이지: 카테고리 항목 클릭 후 편집 화면

                    **플로우:**
                    1. 홈/마이 → 카테고리 항목 클릭 또는 편집 버튼 클릭
                    2. 카테고리 편집 페이지 로드
                    3. 주기(cycleDays), 이름(name), 설명(note) 수정
                    4. PATCH /categories/{categoryId} 호출
                    5. 수정된 카테고리 반환 → 상태 재계산 → UI 업데이트

                    검증:
                    - cycleDays: 3, 7, 14, 21, 28 중 하나만 허용
                    - name: 같은 사용자 내 다른 활성 카테고리와 중복 불가
                    - 소유자(현재 로그인한 사용자)가 아닌 경우 수정 불가
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "카테고리 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "수정된 카테고리",
                                    value = """
                                            {
                                              "data": {
                                                "id": "cat_bath",
                                                "name": "욕실",
                                                "icon": "bath",
                                                "cycleDays": 14,
                                                "lastDoneAt": "2026-07-01T09:00:00+09:00",
                                                "nextDueAt": "2026-07-15T09:00:00+09:00",
                                                "note": "물때와 습기를 기준으로 관리해요.",
                                                "status": {
                                                  "code": "good",
                                                  "label": "5일 뒤 다시 보면 충분해요",
                                                  "daysUntilNext": 5
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (cycleDays가 유효하지 않음 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카테고리를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이름 중복 등 상태 충돌"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<CategoryResponse> update(@PathVariable UUID categoryId,
                                         @Valid @RequestBody UpdateCategoryRequest request);
}
