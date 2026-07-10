package cleanloop.summary;

import cleanloop.common.response.ApiResponse;
import cleanloop.summary.controller.api.SummaryControllerApiSpec;
import cleanloop.summary.dto.HomeResponse;
import cleanloop.summary.dto.MeSummaryResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController implements SummaryControllerApiSpec {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/api/v1/home")
    public ApiResponse<HomeResponse> home(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return ApiResponse.of(summaryService.home(today));
    }

    @GetMapping("/api/v1/me/summary")
    public ApiResponse<MeSummaryResponse> meSummary() {
        return ApiResponse.of(summaryService.meSummary());
    }
}
