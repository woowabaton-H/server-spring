package cleanloop.category.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.category.dto.CategoryPresetResponse;
import cleanloop.common.response.ApiResponse;
import java.util.List;

@Tag(name = "Category", description = "청소 카테고리 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface CategoryPresetControllerApiSpec {

    @Operation(
            summary = "추가 가능한 카테고리 프리셋 목록 조회",
            description = """
                    카테고리 추가 페이지에서 사용자가 선택할 수 있는 프리셋 목록을 조회한다.

                    **사용 페이지:**
                    - 카테고리 추가 페이지: "프리셋 선택" 섹션

                    **플로우:**
                    1. 홈/마이 → "카테고리 추가" 버튼 클릭
                    2. 카테고리 추가 페이지 로드
                    3. GET /category-presets 호출 → 활성 프리셋 목록 수신
                    4. UI에 프리셋 카드/옵션 렌더링
                    5. 사용자가 프리셋 선택 → POST /categories 호출

                    프리셋은:
                    - 반려동물, 욕실, 주방, 세탁/침구, 쓰레기/수거 등 사전 정의된 카테고리
                    - 아이콘, 기본 주기, 설명문구를 포함
                    - sort_order 순으로 정렬되어 제공
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프리셋 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CategoryPresetResponse.class),
                            examples = @ExampleObject(
                                    name = "성공 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "key": "pet",
                                                  "name": "반려동물",
                                                  "icon": "floor",
                                                  "cycleDays": 7,
                                                  "note": "털, 냄새, 패드 주변을 한 카테고리로 관리해요."
                                                },
                                                {
                                                  "key": "kitchen",
                                                  "name": "주방",
                                                  "icon": "kitchen",
                                                  "cycleDays": 7,
                                                  "note": "배수구와 조리대 표면을 기준으로 잡아요."
                                                },
                                                {
                                                  "key": "bath",
                                                  "name": "욕실",
                                                  "icon": "bath",
                                                  "cycleDays": 14,
                                                  "note": "물때와 습기만 잡아도 관리가 쉬워져요."
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
    ApiResponse<List<CategoryPresetResponse>> findPresets();
}
