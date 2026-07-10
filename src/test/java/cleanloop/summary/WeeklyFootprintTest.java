package cleanloop.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WeeklyFootprintTest {

    @ParameterizedTest(name = "{0}건 -> level {1}")
    @CsvSource({"0, 0", "1, 1", "2, 2", "3, 2", "4, 3", "9, 3"})
    void 완료_수를_0에서_3단계로_변환한다(int completionCount, int expectedLevel) {
        WeeklyFootprint footprint = WeeklyFootprint.of(LocalDate.of(2026, 7, 6), completionCount);

        assertThat(footprint.level()).isEqualTo(expectedLevel);
        assertThat(footprint.completionCount()).isEqualTo(completionCount);
    }
}
