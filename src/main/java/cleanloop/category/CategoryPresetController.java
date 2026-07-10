package cleanloop.category;

import cleanloop.category.dto.CategoryPresetResponse;
import cleanloop.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/category-presets")
public class CategoryPresetController {

    private final CategoryService categoryService;

    public CategoryPresetController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryPresetResponse>> findPresets() {
        return ApiResponse.of(categoryService.findPresets());
    }
}
