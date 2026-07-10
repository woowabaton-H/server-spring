package cleanloop.selection;

import java.util.Arrays;

public enum SelectionType {

    KIT("kit", "키트"),
    PRODUCT("product", "용품"),
    SERVICE("service", "서비스"),
    SUBSCRIPTION("subscription", "구독 연결");

    private final String code;
    private final String label;

    SelectionType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 운영자가 넣은 값이 열거형에 없더라도 목록 조회가 깨지지 않도록 코드를 그대로 라벨로 쓴다.
     */
    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .map(type -> type.label)
                .findFirst()
                .orElse(code);
    }
}
