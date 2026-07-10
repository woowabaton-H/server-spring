package cleanloop.user;

import cleanloop.common.response.ApiResponse;
import cleanloop.user.controller.api.UserControllerApiSpec;
import cleanloop.user.dto.UpdateMeRequest;
import cleanloop.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class UserController implements UserControllerApiSpec {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> getMe() {
        return ApiResponse.of(UserResponse.from(userService.getMe()));
    }

    @PatchMapping
    public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateMeRequest request) {
        return ApiResponse.of(UserResponse.from(userService.updateMe(request)));
    }
}
