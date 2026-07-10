package cleanloop.community.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.common.page.PageRequests;
import cleanloop.common.response.ApiResponse;
import cleanloop.community.dto.CommunityPostDetailResponse;
import cleanloop.community.dto.CommunityPostSummaryResponse;
import cleanloop.community.dto.HelpfulResponse;
import cleanloop.community.dto.SavePostResponse;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Community", description = "커뮤니티 게시글, 도움됨, 저장 API")
@SecurityRequirement(name = "bearerAuth")
public interface CommunityControllerApiSpec {

    @Operation(
            summary = "커뮤니티 글 목록 조회",
            description = """
                    사용자들의 청소 팁과 Q&A 글 목록을 조회한다.

                    **사용 페이지:**
                    - 커뮤니티 탭: Tips/Q&A 목록 표시

                    **플로우:**
                    1. 커뮤니티 탭 진입 또는 스크롤
                    2. GET /community/posts (필수: type=tips|qa, 선택: tag, cursor, limit)
                    3. 필터링된 글 목록 수신 (인기도순 정렬)
                    4. 목록 렌더링
                    5. 항목 클릭하면 상세 페이지로 이동

                    필터링:
                    - type: tips(팁) 또는 qa(질문) 필수 선택
                    - tag: 카테고리 태그로 필터링 (욕실, 주방 등)
                    - isPopular=true 항목을 우선 표시

                    정렬:
                    - helpfulCount + savedCount를 기본 인기 기준으로 사용
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "글 목록",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": "tip1",
                                                  "type": "tips",
                                                  "title": "욕실은 세제보다 물기 제거가 먼저였어요",
                                                  "tag": "욕실",
                                                  "bodyPreview": "샤워 후 스퀴지로 30초만 닦아도 물때가 확 줄었습니다.",
                                                  "helpfulCount": 128,
                                                  "commentsCount": 24,
                                                  "answersCount": 0,
                                                  "savedCount": 64,
                                                  "isPopular": true,
                                                  "isSaved": false,
                                                  "createdAt": "2026-07-10T09:00:00+09:00"
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
                    responseCode = "400",
                    description = "잘못된 요청 (type 필수)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse.ListApiResponse<CommunityPostSummaryResponse> findAll(
            @RequestParam String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit);

    @Operation(
            summary = "커뮤니티 글 상세 조회",
            description = """
                    개별 커뮤니티 글의 상세 내용을 조회한다.

                    **사용 페이지:**
                    - 커뮤니티 글 상세 페이지: 글 클릭 후 전체 내용 표시

                    **플로우:**
                    1. 커뮤니티 목록 → 글 항목 클릭
                    2. GET /community/posts/{postId} 호출
                    3. 글 상세 내용 수신
                    4. 상세 페이지 렌더링
                    5. 사용자는 "도움됨", "저장", "댓글/답변 보기" 가능
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "글 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "글을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<CommunityPostDetailResponse> findOne(@PathVariable UUID postId);

    @Operation(
            summary = "글 도움됨 표시",
            description = """
                    커뮤니티 글을 도움이 되었다고 표시한다.

                    **사용 페이지:**
                    - 커뮤니티 글 상세: "도움됨" 버튼 클릭

                    **플로우:**
                    1. 글 상세 페이지 → "도움됨" 버튼 클릭
                    2. PUT /community/posts/{postId}/helpful 호출
                    3. 도움됨 수 1 증가
                    4. UI에 "도움됨" 상태 표시

                    제약:
                    - 사용자당 글 1회만 도움됨 표시 가능
                    - 같은 요청은 멱등적으로 처리 (중복 증가 방지)
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "도움됨 표시 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "글을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<HelpfulResponse> markHelpful(@PathVariable UUID postId);

    @Operation(
            summary = "글 도움됨 취소",
            description = """
                    "도움됨"으로 표시한 글을 취소한다.

                    **사용 페이지:**
                    - 커뮤니티 글 상세: "도움됨" 상태의 버튼 다시 클릭

                    **플로우:**
                    1. "도움됨" 상태의 글 → "도움됨" 버튼 재클릭
                    2. DELETE /community/posts/{postId}/helpful 호출
                    3. 도움됨 수 1 감소
                    4. UI에서 "도움됨" 상태 제거
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "도움됨 취소 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "글을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<HelpfulResponse> unmarkHelpful(@PathVariable UUID postId);

    @Operation(
            summary = "글 저장",
            description = """
                    커뮤니티 글을 저장한다.

                    **사용 페이지:**
                    - 커뮤니티 목록/상세: 북마크 아이콘 클릭
                    - 마이 > 저장한 글: 저장된 글 관리

                    **플로우:**
                    1. 글 항목의 북마크/저장 아이콘 클릭
                    2. PUT /community/posts/{postId}/save 호출
                    3. 저장 상태 갱신
                    4. UI에 "저장됨" 표시
                    5. 마이 > 저장한 글에서 조회 가능

                    멱등성:
                    - 같은 요청은 중복 저장 없이 처리
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "글 저장 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "글을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<SavePostResponse> save(@PathVariable UUID postId);

    @Operation(
            summary = "글 저장 해제",
            description = """
                    저장된 커뮤니티 글의 저장을 해제한다.

                    **사용 페이지:**
                    - 커뮤니티 목록/상세: 저장됨 상태의 북마크 아이콘 다시 클릭
                    - 마이 > 저장한 글: 삭제 버튼 클릭

                    **플로우:**
                    1. 저장된 글의 북마크 아이콘 클릭 (또는 X 버튼)
                    2. DELETE /community/posts/{postId}/save 호출
                    3. 저장 상태 삭제
                    4. UI에서 "저장됨" 표시 제거
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "글 저장 해제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "글을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsave(@PathVariable UUID postId);
}
