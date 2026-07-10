package cleanloop.selection.dto;

/**
 * providerId를 생략하면 셀렉션 자체의 외부 URL로 안내한다.
 */
public record ExternalViewRequest(String providerId) {
}
