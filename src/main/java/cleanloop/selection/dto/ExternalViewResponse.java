package cleanloop.selection.dto;

/**
 * externalUrl은 운영자가 아직 URL을 넣지 않았으면 null이다.
 * 앱 내부에서 결제나 예약 확정은 처리하지 않고, 외부 페이지 확인 고지만 함께 내려준다.
 */
public record ExternalViewResponse(
        String selectionId,
        String providerId,
        String externalUrl,
        String notice
) {
}
