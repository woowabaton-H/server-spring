package cleanloop.selection.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.common.page.PageRequests;
import cleanloop.common.response.ApiResponse;
import cleanloop.selection.dto.ExternalViewRequest;
import cleanloop.selection.dto.ExternalViewResponse;
import cleanloop.selection.dto.SaveSelectionResponse;
import cleanloop.selection.dto.SelectionResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Selection", description = "셀렉션(추천 제품/키트/서비스) 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface SelectionControllerApiSpec {

    @Operation(
            summary = "셀렉션 목록 조회",
            description = """
                    청소 카테고리별 추천 제품/키트/서비스 셀렉션 목록을 조회한다.

                    **사용 페이지:**
                    - 홈 > 셀렉션 섹션: 홈 페이지에서 추천 셀렉션 표시
                    - 셀렉션 탭: 전체 또는 카테고리별 셀렉션 목록 조회

                    **플로우:**
                    1. 홈 페이지 로드 또는 셀렉션 탭 진입
                    2. GET /selections (선택: category, type, cursor, limit)
                    3. 필터링된 셀렉션 목록 수신
                    4. 에디터 픽, 추천 항목을 우선 표시
                    5. 사용자의 저장 상태(isSaved) 함께 표시

                    필터링:
                    - category: 전체/욕실/주방/세탁/쓰레기/바닥/계절 중 선택
                    - type: kit/product/service/subscription 중 선택
                    - category=욕실 요청 시 "욕실" 항목과 "전체" 항목을 함께 반환
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "셀렉션 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "셀렉션 목록",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": "starter-kit",
                                                  "type": "kit",
                                                  "typeLabel": "키트",
                                                  "category": "전체",
                                                  "title": "자취 첫 달 기본 청소 키트",
                                                  "label": "에디터 픽",
                                                  "isHighlighted": true,
                                                  "priceText": "19,000원대",
                                                  "affiliateText": "일부 제휴",
                                                  "imageUrl": "https://cdn.cleanloop.example/selections/starter-kit.jpg",
                                                  "ratingText": "4.8",
                                                  "reviewCountText": "후기 1,240개",
                                                  "tags": ["입문용", "가성비", "한 번에 준비"],
                                                  "reason": "세제 종류를 늘리기보다 매주 쓰는 것만 담았어요.",
                                                  "fitFor": "처음 자취를 시작했거나 청소 도구가 거의 없는 사용자",
                                                  "isSaved": false,
                                                  "providers": []
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
    ApiResponse.ListApiResponse<SelectionResponse> findAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit);

    @Operation(
            summary = "셀렉션 상세 조회",
            description = """
                    개별 셀렉션의 상세 정보를 조회한다. 제공 업체(providers) 정보가 포함된다.

                    **사용 페이지:**
                    - 셀렉션 상세 페이지: 셀렉션 항목 클릭 후 상세 정보 표시

                    **플로우:**
                    1. 셀렉션 목록 → 항목 클릭
                    2. GET /selections/{selectionId} 호출
                    3. 셀렉션 상세 정보 수신 (제휴 업체 리스트, 가격, 링크 등)
                    4. 상세 페이지 렌더링
                    5. 사용자는 "저장", "외부 보기" 버튼 클릭 가능

                    상세 전용 필드:
                    - notice: 최종 조건 확인 고지 문구
                    - checks: 구매나 이용 전 확인할 항목 목록
                    - providers: 연결된 제공업체 후보

                    목록 응답에는 위 세 필드가 없다. 카드 표시용 imageUrl, ratingText,
                    reviewCountText, tags는 목록과 상세 모두에 담긴다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "셀렉션 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "셀렉션을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<SelectionResponse> findOne(@PathVariable String selectionId);

    @Operation(
            summary = "셀렉션 저장",
            description = """
                    셀렉션 상세 또는 목록에서 하트 아이콘을 클릭해 셀렉션을 저장한다.

                    **사용 페이지:**
                    - 셀렉션 목록: 하트 아이콘 클릭
                    - 셀렉션 상세: "저장" 버튼 클릭
                    - 마이 > 저장한 셀렉션: 저장된 항목 관리

                    **플로우:**
                    1. 셀렉션 항목의 "저장" 버튼/아이콘 클릭
                    2. PUT /selections/{selectionId}/save 호출
                    3. 저장 상태 갱신 (isSaved=true)
                    4. UI에 "저장됨" 표시
                    5. 마이 > 저장한 셀렉션에서 조회 가능

                    같은 항목 저장 요청은 멱등적으로 처리되어 중복 저장이 방지된다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "셀렉션 저장 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "셀렉션을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<SaveSelectionResponse> save(@PathVariable String selectionId);

    @Operation(
            summary = "셀렉션 저장 해제",
            description = """
                    저장된 셀렉션의 저장을 해제한다.

                    **사용 페이지:**
                    - 셀렉션 목록/상세: 저장됨 상태의 하트 아이콘 다시 클릭
                    - 마이 > 저장한 셀렉션: 저장 해제 버튼 클릭

                    **플로우:**
                    1. 저장된 셀렉션의 하트 아이콘 클릭 (또는 X 버튼)
                    2. DELETE /selections/{selectionId}/save 호출
                    3. 저장 상태 삭제
                    4. UI에서 "저장됨" 표시 제거
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "셀렉션 저장 해제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "셀렉션을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsave(@PathVariable String selectionId);

    @Operation(
            summary = "외부 링크 클릭 기록",
            description = """
                    셀렉션 상세 페이지에서 "외부 보기" 또는 "제휴사 링크" 클릭을 기록한다.

                    **사용 페이지:**
                    - 셀렉션 상세: "외부 보기", "구매하기", "구독하기" 링크 클릭

                    **플로우:**
                    1. 셀렉션 상세 페이지 → "외부 보기" 버튼 클릭
                    2. POST /selections/{selectionId}/external-view 호출 (선택: providerId)
                    3. 클릭 이벤트 기록 (분석용)
                    4. 외부 URL 반환 (있을 경우)
                    5. 사용자를 외부 페이지로 리다이렉트

                    MVP 규칙:
                    - 앱 내부에서 결제/예약 처리 안 함
                    - 클릭 이벤트만 기록
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "외부 링크 클릭 기록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "셀렉션 또는 제휴사를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<ExternalViewResponse> externalView(
            @PathVariable String selectionId,
            @RequestBody(required = false) ExternalViewRequest request);

    @Operation(
            summary = "저장한 셀렉션 조회",
            description = """
                    마이 페이지에서 사용자가 저장한 셀렉션 목록을 조회한다.

                    **사용 페이지:**
                    - 마이 > 저장한 셀렉션: 저장된 셀렉션 목록 표시

                    **플로우:**
                    1. 마이 페이지 → "저장한 셀렉션" 탭 진입
                    2. GET /me/saved-selections 호출
                    3. 사용자가 저장한 모든 셀렉션 수신
                    4. 목록 렌더링
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "저장한 셀렉션 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            )
    })
    ApiResponse<List<SelectionResponse>> findSaved();
}
