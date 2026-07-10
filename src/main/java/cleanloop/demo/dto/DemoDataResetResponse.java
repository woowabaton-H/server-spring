package cleanloop.demo.dto;

import java.util.List;
import java.util.Map;

public record DemoDataResetResponse(
        String message,
        List<String> scripts,
        Map<String, Integer> tableCounts
) {
}
