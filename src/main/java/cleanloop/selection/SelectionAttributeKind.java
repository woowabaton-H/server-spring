package cleanloop.selection;

/**
 * selection_attributes에 함께 저장되는 문자열 목록의 종류.
 */
public enum SelectionAttributeKind {

    /** 카드에 붙는 짧은 태그. */
    TAG("tag"),

    /** 구매나 이용 전 확인할 항목. notice의 배열 버전이다. */
    CHECK("check");

    private final String code;

    SelectionAttributeKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** 운영자가 넣은 값이 열거형에 없으면 무시한다. 알 수 없는 종류 때문에 조회가 깨지지 않게 한다. */
    public static SelectionAttributeKind from(String code) {
        for (SelectionAttributeKind kind : values()) {
            if (kind.code.equals(code)) {
                return kind;
            }
        }
        return null;
    }
}
