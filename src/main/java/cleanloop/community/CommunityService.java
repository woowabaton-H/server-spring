package cleanloop.community;

import cleanloop.common.error.ApiException;
import cleanloop.common.error.ErrorCode;
import cleanloop.common.page.CursorCodec;
import cleanloop.community.dto.CommentResponse;
import cleanloop.community.dto.CommunityPostDetailResponse;
import cleanloop.community.dto.CommunityPostSummaryResponse;
import cleanloop.community.dto.CreateCommentRequest;
import cleanloop.community.dto.CreatePostRequest;
import cleanloop.community.dto.HelpfulResponse;
import cleanloop.community.dto.SavePostResponse;
import cleanloop.user.User;
import cleanloop.user.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

    private final CommunityPostRepository postRepository;
    private final CommunityReactionRepository reactionRepository;
    private final CommunityCommentRepository commentRepository;
    private final UserService userService;

    public CommunityService(CommunityPostRepository postRepository,
                            CommunityReactionRepository reactionRepository,
                            CommunityCommentRepository commentRepository,
                            UserService userService) {
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
    }

    /**
     * 작성자는 현재 사용자다. 새 글은 반응과 댓글이 없으므로 모든 집계가 0에서 시작한다.
     */
    @Transactional
    public CommunityPostDetailResponse createPost(CreatePostRequest request) {
        User user = userService.getMe();
        PostType type = PostType.from(request.type());

        UUID postId = UUID.randomUUID();
        postRepository.insert(postId, type, request.title(), request.tag(), request.body(), user.id());

        CommunityPost created = findPublished(postId);
        return CommunityPostDetailResponse.of(created, false, false, user.timezone());
    }

    @Transactional(readOnly = true)
    public CommentPage findComments(UUID postId, String cursor, int limit) {
        User user = userService.getMe();
        findPublished(postId);

        List<CommunityComment> comments = commentRepository.findPage(postId, decodeCommentCursor(cursor), limit);
        boolean hasNext = comments.size() > limit;
        if (hasNext) {
            comments = comments.subList(0, limit);
        }
        String nextCursor = hasNext ? encodeCommentCursor(comments.get(comments.size() - 1)) : null;

        List<CommentResponse> responses = comments.stream()
                .map(comment -> CommentResponse.of(comment, user.id(), user.timezone()))
                .toList();
        return new CommentPage(responses, nextCursor);
    }

    /**
     * 댓글 삽입과 집계 컬럼 증가를 한 트랜잭션으로 묶는다.
     * tips 글이면 commentsCount가, qa 글이면 answersCount가 오른다.
     */
    @Transactional
    public CommentResponse addComment(UUID postId, CreateCommentRequest request) {
        User user = userService.getMe();
        CommunityPost post = findPublished(postId);

        CommunityComment comment = commentRepository.insert(
                UUID.randomUUID(), postId, user.id(), request.body());
        postRepository.incrementCommentCounter(postId, post.postType());

        return CommentResponse.of(comment, user.id(), user.timezone());
    }

    @Transactional(readOnly = true)
    public Page findAll(String type, String tag, String cursor, int limit) {
        User user = userService.getMe();
        PostType postType = PostType.from(type);

        List<CommunityPost> posts = postRepository.findPage(postType, tag, decodeCursor(cursor), limit);
        boolean hasNext = posts.size() > limit;
        if (hasNext) {
            posts = posts.subList(0, limit);
        }
        String nextCursor = hasNext ? encodeCursor(posts.get(posts.size() - 1)) : null;

        // 상위 N번째 점수를 문턱값으로 쓰면 어느 페이지에서 보든, 어떤 tag로 좁혀도 판정이 같다.
        // 글이 N건보다 적으면 문턱값이 없으므로 모두 인기 글로 본다.
        int popularThreshold = postRepository.findPopularScoreThreshold(postType).orElse(0);
        Set<UUID> savedIds = reactionRepository.findReactedPostIds(user.id(), ReactionType.SAVE);

        List<CommunityPostSummaryResponse> responses = posts.stream()
                .map(post -> CommunityPostSummaryResponse.of(
                        post,
                        post.popularScore() >= popularThreshold,
                        savedIds.contains(post.id()),
                        user.timezone()))
                .toList();
        return new Page(responses, nextCursor);
    }

    @Transactional(readOnly = true)
    public CommunityPostDetailResponse findOne(UUID postId) {
        User user = userService.getMe();
        CommunityPost post = findPublished(postId);

        return CommunityPostDetailResponse.of(
                post,
                reactionRepository.exists(user.id(), postId, ReactionType.SAVE),
                reactionRepository.exists(user.id(), postId, ReactionType.HELPFUL),
                user.timezone());
    }

    /**
     * 사용자당 1회. 반응 행이 새로 삽입된 경우에만 카운터를 올린다.
     * 반복 호출해도 결과가 같다.
     */
    @Transactional
    public HelpfulResponse markHelpful(UUID postId) {
        User user = userService.getMe();
        findPublished(postId);

        if (reactionRepository.insertIfAbsent(user.id(), postId, ReactionType.HELPFUL)) {
            postRepository.incrementCounter(postId, ReactionType.HELPFUL);
        }
        return new HelpfulResponse(postId.toString(), true, reloadHelpfulCount(postId));
    }

    /** 반응 행이 실제로 삭제된 경우에만 카운터를 내린다. */
    @Transactional
    public HelpfulResponse unmarkHelpful(UUID postId) {
        User user = userService.getMe();
        findPublished(postId);

        if (reactionRepository.delete(user.id(), postId, ReactionType.HELPFUL)) {
            postRepository.decrementCounter(postId, ReactionType.HELPFUL);
        }
        return new HelpfulResponse(postId.toString(), false, reloadHelpfulCount(postId));
    }

    /** 카운터 갱신은 SQL로 하므로, 응답에 담을 값은 다시 읽어온다. */
    private int reloadHelpfulCount(UUID postId) {
        return findPublished(postId).helpfulCount();
    }

    @Transactional
    public SavePostResponse save(UUID postId) {
        User user = userService.getMe();
        findPublished(postId);

        if (reactionRepository.insertIfAbsent(user.id(), postId, ReactionType.SAVE)) {
            postRepository.incrementCounter(postId, ReactionType.SAVE);
        }
        LocalDateTime savedAt = reactionRepository.findCreatedAt(user.id(), postId, ReactionType.SAVE).orElseThrow();
        return new SavePostResponse(postId.toString(), true, savedAt.atZone(user.timezone()).toOffsetDateTime());
    }

    @Transactional
    public void unsave(UUID postId) {
        User user = userService.getMe();
        findPublished(postId);

        if (reactionRepository.delete(user.id(), postId, ReactionType.SAVE)) {
            postRepository.decrementCounter(postId, ReactionType.SAVE);
        }
    }

    private CommunityPost findPublished(UUID postId) {
        return postRepository.findPublishedById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMUNITY_POST_NOT_FOUND));
    }

    private String encodeCursor(CommunityPost post) {
        return CursorCodec.encode(post.popularScore() + "|" + post.createdAt() + "|" + post.id());
    }

    private CommunityPostRepository.Cursor decodeCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        String[] parts = CursorCodec.decode(cursor).split("\\|", 3);
        if (parts.length != 3) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
        try {
            return new CommunityPostRepository.Cursor(
                    Integer.parseInt(parts[0]),
                    LocalDateTime.parse(parts[1]),
                    UUID.fromString(parts[2]));
        } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
    }

    private String encodeCommentCursor(CommunityComment comment) {
        return CursorCodec.encode(comment.createdAt() + "|" + comment.id());
    }

    private CommunityCommentRepository.Cursor decodeCommentCursor(String cursor) {
        if (cursor == null) {
            return null;
        }
        String[] parts = CursorCodec.decode(cursor).split("\\|", 2);
        if (parts.length != 2) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
        try {
            return new CommunityCommentRepository.Cursor(
                    LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (java.time.format.DateTimeParseException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_CURSOR);
        }
    }

    public record Page(List<CommunityPostSummaryResponse> posts, String nextCursor) {
    }

    public record CommentPage(List<CommentResponse> comments, String nextCursor) {
    }
}
