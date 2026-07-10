package cleanloop.selection;

import cleanloop.common.page.PageRequests;
import cleanloop.common.response.ApiResponse;
import cleanloop.selection.controller.api.SelectionControllerApiSpec;
import cleanloop.selection.dto.ExternalViewRequest;
import cleanloop.selection.dto.ExternalViewResponse;
import cleanloop.selection.dto.SaveSelectionResponse;
import cleanloop.selection.dto.SelectionResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SelectionController implements SelectionControllerApiSpec {

    private final SelectionService selectionService;

    public SelectionController(SelectionService selectionService) {
        this.selectionService = selectionService;
    }

    @GetMapping("/api/v1/selections")
    public ApiResponse.ListApiResponse<SelectionResponse> findAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        SelectionService.Page page =
                selectionService.findAll(category, type, cursor, PageRequests.normalizeLimit(limit));
        return ApiResponse.list(page.selections(), page.nextCursor());
    }

    @GetMapping("/api/v1/selections/{selectionId}")
    public ApiResponse<SelectionResponse> findOne(@PathVariable String selectionId) {
        return ApiResponse.of(selectionService.findBySlug(selectionId));
    }

    @PutMapping("/api/v1/selections/{selectionId}/save")
    public ApiResponse<SaveSelectionResponse> save(@PathVariable String selectionId) {
        return ApiResponse.of(selectionService.save(selectionId));
    }

    @DeleteMapping("/api/v1/selections/{selectionId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable String selectionId) {
        selectionService.unsave(selectionId);
    }

    @PostMapping("/api/v1/selections/{selectionId}/external-view")
    public ApiResponse<ExternalViewResponse> externalView(
            @PathVariable String selectionId,
            @RequestBody(required = false) ExternalViewRequest request) {
        return ApiResponse.of(selectionService.recordExternalView(selectionId, request));
    }

    @GetMapping("/api/v1/me/saved-selections")
    public ApiResponse<List<SelectionResponse>> findSaved() {
        return ApiResponse.of(selectionService.findSaved());
    }
}
