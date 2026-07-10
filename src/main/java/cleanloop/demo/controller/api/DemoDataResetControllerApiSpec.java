package cleanloop.demo.controller.api;

import cleanloop.common.response.ApiResponse;
import cleanloop.demo.dto.DemoDataResetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Demo", description = "해커톤 시연용 데이터 관리 API")
public interface DemoDataResetControllerApiSpec {

    @Operation(
            summary = "시연 데이터 초기화",
            description = """
                    현재 H2 데이터베이스의 모든 테이블을 삭제하고 schema.sql, data.sql 기반 시드 상태로 다시 채운다.

                    **주의:**
                    - 해커톤 시연용 엔드포인트다.
                    - 기존 완료 기록, 저장 항목, 커뮤니티 반응, 프로필 변경은 모두 시드 상태로 되돌아간다.
                    - doc-data.sql이 배포 산출물에 있으면 함께 실행하고, 없으면 건너뛴다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "시드 데이터 복구 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "복구 결과",
                                    value = """
                                            {
                                              "data": {
                                                "message": "시드 데이터로 복구했습니다.",
                                                "scripts": ["schema.sql", "data.sql"],
                                                "tableCounts": {
                                                  "users": 1,
                                                  "cleaning_categories": 6
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    ApiResponse<DemoDataResetResponse> reset();
}
