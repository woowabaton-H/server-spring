package cleanloop.category;

import cleanloop.category.dto.CategoryResponse;
import cleanloop.category.dto.CreateCategoryRequest;
import cleanloop.category.dto.UpdateCategoryRequest;
import cleanloop.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.of(categoryService.findAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.of(categoryService.create(request));
    }

    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID categoryId,
                                                @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.of(categoryService.update(categoryId, request));
    }
}
