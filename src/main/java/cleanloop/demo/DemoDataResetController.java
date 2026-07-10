package cleanloop.demo;

import cleanloop.common.response.ApiResponse;
import cleanloop.demo.controller.api.DemoDataResetControllerApiSpec;
import cleanloop.demo.dto.DemoDataResetResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoDataResetController implements DemoDataResetControllerApiSpec {

    private final DemoDataResetService demoDataResetService;

    public DemoDataResetController(DemoDataResetService demoDataResetService) {
        this.demoDataResetService = demoDataResetService;
    }

    @PostMapping("/api/v1/admin/demo-data/reset")
    public ApiResponse<DemoDataResetResponse> reset() {
        return ApiResponse.of(demoDataResetService.reset());
    }
}
