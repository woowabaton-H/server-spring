package cleanloop.user.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.common.response.ApiResponse;
import cleanloop.user.dto.UpdateMeRequest;
import cleanloop.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "사용자 정보 조회 및 수정 API")
@SecurityRequirement(name = "bearerAuth")
public interface UserControllerApiSpec {

    @Operation(
            summary = "내 정보 조회",
            description = """
                    현재 로그인한 사용자의 정보를 조회한다.

                    **사용 페이지:**
                    - 마이 페이지: 사용자 프로필 표시
                    - 프로필 편집 페이지: 편집 전 현재 정보 표시

                    **플로우:**
                    1. 마이 탭 진입 또는 프로필 편집 페이지 로드
                    2. GET /me 호출
                    3. 사용자 정보 수신 (이름, 아바타, 타임존, 생성일)
                    4. 프로필 UI 렌더링

                    반환 정보:
                    - id: 사용자 UUID
                    - name: 표시 이름
                    - avatarText: 아바타 문자 (1-2자)
                    - timezone: 사용자 타임존 (기본: Asia/Seoul)
                    - createdAt: 가입 시각
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "사용자 정보",
                                    value = """
                                            {
                                              "data": {
                                                "id": "usr_01",
                                                "name": "김보송",
                                                "avatarText": "보",
                                                "timezone": "Asia/Seoul",
                                                "createdAt": "2026-07-10T09:00:00+09:00"
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
    ApiResponse<UserResponse> getMe();

    @Operation(
            summary = "내 정보 수정",
            description = """
                    현재 로그인한 사용자의 이름과 아바타 정보를 수정한다.

                    **사용 페이지:**
                    - 프로필 편집 페이지: 이름, 아바타 수정 후 저장

                    **플로우:**
                    1. 마이 페이지 → 프로필 편집 버튼 클릭
                    2. 프로필 편집 페이지 로드
                    3. 이름(name) 및/또는 아바타(avatarText) 수정
                    4. PATCH /me 호출 (변경 필드만 전송)
                    5. 수정된 정보 반환
                    6. 마이 페이지 및 홈의 프로필 정보 즉시 업데이트

                    검증:
                    - name: 1-40자
                    - avatarText: 1-2자 (한글/영문 지원)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "내 정보 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "수정된 사용자 정보",
                                    value = """
                                            {
                                              "data": {
                                                "id": "usr_01",
                                                "name": "보송",
                                                "avatarText": "보",
                                                "timezone": "Asia/Seoul"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (이름, 아바타 형식 오류)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateMeRequest request);
}
