package cleanloop.demo;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoDataResetApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 시연_데이터를_시드_상태로_복구한다() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "시연중", "avatarText": "시"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"presetKey": "pet"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.data.length()").value(7));

        mockMvc.perform(post("/api/v1/admin/demo-data/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("시드 데이터로 복구했습니다."))
                .andExpect(jsonPath("$.data.scripts").value(hasItem("schema.sql")))
                .andExpect(jsonPath("$.data.scripts").value(hasItem("data.sql")))
                .andExpect(jsonPath("$.data.tableCounts.users").value(1))
                .andExpect(jsonPath("$.data.tableCounts.cleaning_categories").value(6))
                .andExpect(jsonPath("$.data.tableCounts.completion_logs").value(10));

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("보송"))
                .andExpect(jsonPath("$.data.avatarText").value("보"));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].name").value("욕실"));
    }
}
