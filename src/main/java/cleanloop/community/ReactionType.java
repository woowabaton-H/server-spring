package cleanloop.community;

public enum ReactionType {

    HELPFUL("helpful", "helpful_count"),
    SAVE("save", "saved_count");

    private final String code;
    private final String counterColumn;

    ReactionType(String code, String counterColumn) {
        this.code = code;
        this.counterColumn = counterColumn;
    }

    public String code() {
        return code;
    }

    /** 반응 종류마다 갱신할 집계 컬럼이 다르다. 열거형이 소유하므로 외부 입력이 섞일 수 없다. */
    public String counterColumn() {
        return counterColumn;
    }
}
