package cleanloop.community;

import cleanloop.common.page.PageRequests;
import cleanloop.common.response.ApiResponse;
import cleanloop.community.controller.api.CommunityControllerApiSpec;
import cleanloop.community.dto.CommentResponse;
import cleanloop.community.dto.CommunityPostDetailResponse;
import cleanloop.community.dto.CommunityPostSummaryResponse;
import cleanloop.community.dto.CreateCommentRequest;
import cleanloop.community.dto.CreatePostRequest;
import cleanloop.community.dto.HelpfulResponse;
import cleanloop.community.dto.SavePostResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/community/posts")
public class CommunityController implements CommunityControllerApiSpec {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ApiResponse.ListApiResponse<CommunityPostSummaryResponse> findAll(
            @RequestParam String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        CommunityService.Page page =
                communityService.findAll(type, tag, cursor, PageRequests.normalizeLimit(limit));
        return ApiResponse.list(page.posts(), page.nextCursor());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityPostDetailResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        return ApiResponse.of(communityService.createPost(request));
    }

    @GetMapping("/{postId}")
    public ApiResponse<CommunityPostDetailResponse> findOne(@PathVariable UUID postId) {
        return ApiResponse.of(communityService.findOne(postId));
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse.ListApiResponse<CommentResponse> findComments(
            @PathVariable UUID postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        CommunityService.CommentPage page =
                communityService.findComments(postId, cursor, PageRequests.normalizeLimit(limit));
        return ApiResponse.list(page.comments(), page.nextCursor());
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> addComment(@PathVariable UUID postId,
                                                   @Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.of(communityService.addComment(postId, request));
    }

    @PutMapping("/{postId}/helpful")
    public ApiResponse<HelpfulResponse> markHelpful(@PathVariable UUID postId) {
        return ApiResponse.of(communityService.markHelpful(postId));
    }

    @DeleteMapping("/{postId}/helpful")
    public ApiResponse<HelpfulResponse> unmarkHelpful(@PathVariable UUID postId) {
        return ApiResponse.of(communityService.unmarkHelpful(postId));
    }

    @PutMapping("/{postId}/save")
    public ApiResponse<SavePostResponse> save(@PathVariable UUID postId) {
        return ApiResponse.of(communityService.save(postId));
    }

    @DeleteMapping("/{postId}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable UUID postId) {
        communityService.unsave(postId);
    }
}
