package cleanloop.notification.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import cleanloop.common.response.ApiResponse;
import cleanloop.notification.dto.NotificationListResponse;
import cleanloop.notification.dto.NotificationReadResponse;
import cleanloop.notification.dto.ReadAllNotificationsResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Notification", description = "앱 내부 알림 목록과 읽음 처리 API")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationControllerApiSpec {

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    사용자의 앱 내부 알림 목록과 미확인 수를 함께 조회한다.

                    **사용 페이지:**
                    - 홈: 종 아이콘을 눌러 여는 알림함

                    **플로우:**
                    1. 홈의 종 아이콘 클릭
                    2. GET /notifications 호출
                    3. 알림 목록과 unreadCount 수신
                    4. 알림함 렌더링, 항목 클릭 시 deepLink로 이동

                    정렬:
                    - 미확인 알림 우선, 그 안에서 createdAt 최신순

                    문구:
                    - 압박형이 아니라 제안형으로 생성된다 ("이번 주에는 욕실만 챙겨도 충분해요")

                    unreadCount:
                    - meta가 아니라 data 안에 담는다. meta에는 requestId 같은 전송 계층 값만 싣는다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "알림 목록",
                                    value = """
                                            {
                                              "data": {
                                                "unreadCount": 1,
                                                "notifications": [
                                                  {
                                                    "id": "11000000-0000-0000-0000-000000000001",
                                                    "categoryId": "b0000000-0000-0000-0000-000000000001",
                                                    "categoryName": "욕실",
                                                    "title": "이번 주에는 욕실만 챙겨도 충분해요",
                                                    "body": "욕실 카테고리를 이번 주 안에 한 번 완료하면 다음 관리는 자동으로 다시 잡아둘게요.",
                                                    "deepLink": "/categories/b0000000-0000-0000-0000-000000000001",
                                                    "isRead": false,
                                                    "createdAt": "2026-07-08T09:00:00+09:00"
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
    ApiResponse<NotificationListResponse> findAll();

    @Operation(
            summary = "알림 읽음 처리",
            description = """
                    개별 알림을 읽음으로 표시한다.

                    **사용 페이지:**
                    - 알림함: 알림 항목 클릭

                    **플로우:**
                    1. 알림함에서 알림 클릭
                    2. POST /notifications/{notificationId}/read 호출
                    3. 읽음 상태와 readAt 수신
                    4. deepLink 화면으로 이동, 홈의 미확인 배지 갱신

                    멱등성:
                    - 이미 읽은 알림에 다시 호출해도 200이며, 처음 읽은 시각(readAt)은 바뀌지 않는다
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "읽음 처리",
                                    value = """
                                            {
                                              "data": {
                                                "id": "11000000-0000-0000-0000-000000000001",
                                                "isRead": true,
                                                "readAt": "2026-07-10T12:00:00+09:00"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "알림을 찾을 수 없음 (다른 사용자의 알림 포함)"
            )
    })
    ApiResponse<NotificationReadResponse> read(@PathVariable UUID notificationId);

    @Operation(
            summary = "알림 전체 읽음 처리",
            description = """
                    미확인 알림을 한 번에 모두 읽음으로 표시한다.

                    **사용 페이지:**
                    - 알림함: "모두 읽음" 버튼

                    **플로우:**
                    1. 알림함에서 "모두 읽음" 클릭
                    2. PUT /notifications/read-all 호출
                    3. updatedCount와 unreadCount(항상 0) 수신
                    4. 홈을 다시 부르지 않고 미확인 배지를 지운다

                    주의:
                    - 알림함을 열 때 자동 호출하지 않는 것을 권장한다.
                      전체 읽음 처리는 "같은 카테고리에 미확인 알림이 있으면 새 알림을 만들지 않는다"는
                      생성 규칙을 풀어버려, 다음 날 같은 알림이 다시 쌓인다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "전체 읽음 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "전체 읽음 처리",
                                    value = """
                                            {
                                              "data": {
                                                "updatedCount": 1,
                                                "unreadCount": 0
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
    ApiResponse<ReadAllNotificationsResponse> readAll();
}
