package cleanloop.community.dto;

public record HelpfulResponse(String postId, boolean hasMarkedHelpful, int helpfulCount) {
}
