package cleanloop.notification;

import cleanloop.common.response.ApiResponse;
import cleanloop.notification.controller.api.NotificationControllerApiSpec;
import cleanloop.notification.dto.NotificationListResponse;
import cleanloop.notification.dto.NotificationReadResponse;
import cleanloop.notification.dto.ReadAllNotificationsResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationControllerApiSpec {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<NotificationListResponse> findAll() {
        return ApiResponse.of(notificationService.findAll());
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationReadResponse> read(@PathVariable UUID notificationId) {
        return ApiResponse.of(notificationService.read(notificationId));
    }

    @PutMapping("/read-all")
    public ApiResponse<ReadAllNotificationsResponse> readAll() {
        return ApiResponse.of(notificationService.readAll());
    }
}
