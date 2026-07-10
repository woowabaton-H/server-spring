package cleanloop.demo;

import cleanloop.demo.dto.DemoDataResetResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

@Service
public class DemoDataResetService {

    private static final List<String> RESET_SCRIPT_NAMES = List.of("schema.sql", "data.sql", "doc-data.sql");
    private static final List<String> COUNT_TABLES = List.of(
            "users",
            "category_presets",
            "cleaning_categories",
            "completion_logs",
            "selection_items",
            "selection_attributes",
            "provider_options",
            "saved_selections",
            "community_posts",
            "community_reactions",
            "community_comments",
            "notifications"
    );

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataResetService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    public synchronized DemoDataResetResponse reset() {
        List<String> executedScripts = existingScripts();

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        executedScripts.stream()
                .map(ClassPathResource::new)
                .forEach(populator::addScript);
        DatabasePopulatorUtils.execute(populator, dataSource);

        return new DemoDataResetResponse(
                "시드 데이터로 복구했습니다.",
                executedScripts,
                tableCounts()
        );
    }

    private List<String> existingScripts() {
        List<String> scripts = new ArrayList<>();
        for (String scriptName : RESET_SCRIPT_NAMES) {
            Resource resource = new ClassPathResource(scriptName);
            if (resource.exists()) {
                scripts.add(scriptName);
            }
        }
        return scripts;
    }

    private Map<String, Integer> tableCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : COUNT_TABLES) {
            counts.put(table, jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class));
        }
        return counts;
    }
}
