package cleanloop.completion;

import cleanloop.common.page.PageRequests;
import cleanloop.common.response.ApiResponse;
import cleanloop.completion.controller.api.CompletionControllerApiSpec;
import cleanloop.completion.dto.CompleteCategoryRequest;
import cleanloop.completion.dto.CompleteCategoryResponse;
import cleanloop.completion.dto.CompletionLogResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CompletionController implements CompletionControllerApiSpec {

    private final CompletionService completionService;

    public CompletionController(CompletionService completionService) {
        this.completionService = completionService;
    }

    @PostMapping("/api/v1/categories/{categoryId}/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompleteCategoryResponse> complete(
            @PathVariable UUID categoryId,
            @RequestBody(required = false) CompleteCategoryRequest request) {
        return ApiResponse.of(completionService.complete(categoryId, request));
    }

    @GetMapping("/api/v1/completion-logs")
    public ApiResponse.ListApiResponse<CompletionLogResponse> findLogs(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        CompletionService.Page page =
                completionService.findLogs(from, to, cursor, PageRequests.normalizeLimit(limit));
        return ApiResponse.list(page.logs(), page.nextCursor());
    }
}
