package cleanloop.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 단건 응답의 meta. requestId만 담는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Meta(String requestId) {
}
