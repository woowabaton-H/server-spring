# Swagger API 명세 작성 패턴 가이드

## 개요

이 가이드는 각 도메인의 API 명세를 **기능 + 사용 페이지 + 플로우** 중심으로 작성하는 패턴을 설명합니다.

**목표**: 스웨거 UI에 접속했을 때, 각 API가 어떤 기능을 하고, 어느 페이지에서, 어떤 플로우로 사용되는지 명확히 파악할 수 있도록 한다.

**범위**: API의 디스크립션 정보만 작성. DTO 세부 필드나 반환값 구조는 스웨거의 스키마로 자동 생성된다.

---

## 패턴 구조

### 1. ControllerApiSpec 인터페이스 작성

각 도메인의 컨트롤러마다 `{Domain}ControllerApiSpec` 인터페이스를 작성한다.

**파일 위치**:
```
src/main/java/cleanloop/{domain}/controller/api/{Domain}ControllerApiSpec.java
```

**구성**:
```java
@Tag(name = "도메인명", description = "도메인 설명")
@SecurityRequirement(name = "bearerAuth")  // 인증 필요 시
public interface {Domain}ControllerApiSpec {
    
    @Operation(
        summary = "간단한 기능 제목",
        description = "자세한 기능 설명"
    )
    @ApiResponses(value = { ... })
    ResponseEntity<> methodName(...);
}
```

---

## Operation 어노테이션 작성 규칙

### summary
- **길이**: 한 줄, 15-30자
- **내용**: "무엇을 조회/수정/생성하는가"
- **예**: 
  - "활성 카테고리 목록 조회" ✓
  - "관리 중인 카테고리 목록을 조회한다" ✗ (너무 길음)

### description
```
기능 설명 (1-2줄)

**사용 페이지:**
- 페이지명: 사용 시점
- 페이지명: 사용 시점

**플로우:**
1. 사용자 액션
2. API 호출
3. 응답 처리
4. UI 업데이트

추가 비즈니스 규칙 (있으면 추가)
```

**예시**:
```
활성 카테고리 목록 조회. 홈과 마이 페이지에서 사용자가 관리 중인 활성 카테고리를 표시한다.

**사용 페이지:**
- 홈: 오늘의 청소 체크리스트 표시 시
- 마이: 관리 중인 카테고리 요약 표시 시

**플로우:**
1. 앱 진입 → 홈 로드
2. GET /categories 호출 → 활성 카테고리 목록 수신
3. 각 카테고리의 상태(due/soon/good)를 계산해 UI에 렌더링

카테고리는 sort_order 기준으로 정렬되며, 각 항목에는 다음 관리 예정일과 현재 상태가 포함된다.
```

---

## ApiResponse 어노테이션 작성 규칙

### 성공 응답 (200/201)
```java
@ApiResponse(
    responseCode = "200",  // 또는 "201"
    description = "조회/생성/수정 성공",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ResponseDTO.class),
        examples = @ExampleObject(
            name = "성공 응답 예시",
            value = """
            {
              "data": { ... }
            }
            """
        )
    )
)
```

**포인트**:
- 스키마는 실제 DTO 클래스로 지정 (필드 설명은 자동 생성)
- `examples`는 실제 요청/응답 예제만 제시
- 세부 필드 설명 주석은 DTO 클래스에 `@Schema`로 작성

### 에러 응답 (400/401/404/409 등)
```java
@ApiResponse(
    responseCode = "400",
    description = "잘못된 요청 (예: cycleDays가 유효하지 않음)"
),
@ApiResponse(
    responseCode = "409",
    description = "중복 또는 상태 충돌 (예: 같은 카테고리가 이미 존재)"
),
@ApiResponse(
    responseCode = "401",
    description = "인증되지 않은 사용자"
)
```

**포인트**:
- 단순 설명만 (스키마 불필요)
- 어떤 **조건**에서 이 에러가 발생하는지 명시

---

## 컨트롤러에서 인터페이스 상속

```java
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController implements CategoryControllerApiSpec {
    // 구현 로직
}
```

**효과**:
- 스웨거는 인터페이스의 어노테이션을 읽어 명세 생성
- 컨트롤러 메서드에는 비즈니스 로직만 작성 (명세 어노테이션 불필요)

---

## 도메인별 적용 체크리스트

각 도메인마다 다음을 작성한다:

### 1단계: ControllerApiSpec 인터페이스
- [ ] 인터페이스 파일 생성
- [ ] `@Tag` 정의 (도메인명, 설명)
- [ ] `@SecurityRequirement(name = "bearerAuth")` 추가 (인증 필요 시)

### 2단계: 각 메서드별 명세
- [ ] `@Operation(summary, description)` 작성
  - [ ] summary: 15-30자, 간결하게
  - [ ] description: 기능 + 사용 페이지 + 플로우 + 규칙
- [ ] `@ApiResponses` 작성
  - [ ] 성공 응답 (200/201)에 예제 포함
  - [ ] 에러 응답 (400/401/404/409 등) 조건 명시

### 3단계: 컨트롤러 적용
- [ ] 컨트롤러 클래스가 인터페이스 구현
- [ ] 컨트롤러 메서드에서 명세 어노테이션 제거 (선택)

---

## Category 도메인 예시

### 파일 구조
```
src/main/java/cleanloop/category/
├── controller/
│   ├── api/
│   │   ├── CategoryControllerApiSpec.java        ← 명세
│   │   └── CategoryPresetControllerApiSpec.java  ← 명세
│   ├── CategoryController.java                   ← 구현
│   └── CategoryPresetController.java             ← 구현
├── CategoryService.java
├── CategoryRepository.java
└── dto/
    ├── CategoryResponse.java
    └── CreateCategoryRequest.java
```

### CategoryControllerApiSpec 예시
- **GET /categories**: 활성 카테고리 목록 조회
  - 사용 페이지: 홈, 마이
  - 플로우: 페이지 로드 → API 호출 → 목록 렌더링

- **POST /categories**: 프리셋 또는 직접 입력으로 카테고리 생성
  - 사용 페이지: 카테고리 추가 페이지
  - 플로우: 프리셋 선택 → 저장 → 목록에 즉시 추가

- **PATCH /categories/{categoryId}**: 카테고리 수정
  - 사용 페이지: 카테고리 편집 페이지
  - 플로우: 카테고리 클릭 → 편집 페이지 → 필드 수정 → 저장

### CategoryPresetControllerApiSpec 예시
- **GET /category-presets**: 추가 가능한 프리셋 목록 조회
  - 사용 페이지: 카테고리 추가 페이지
  - 플로우: 추가 페이지 로드 → 프리셋 목록 표시 → 사용자 선택

---

## 다른 도메인 적용 예시

### Completion 도메인
```java
@Operation(
    summary = "카테고리 완료 처리",
    description = """
    홈 페이지의 완료 버튼을 클릭해 카테고리를 완료 처리한다.
    
    **사용 페이지:**
    - 홈: 완료 체크리스트
    
    **플로우:**
    1. 홈 → 카테고리 "완료" 버튼 클릭
    2. POST /categories/{categoryId}/complete 호출
    3. 완료 기록 생성 + 카테고리 상태 갱신
    4. 토스트 메시지 표시 (예: "욕실 완료. 다음 관리는 7월 24일")
    
    이 요청은 트랜잭션으로 처리되며, 완료 기록과 카테고리 상태가 함께 갱신된다.
    """
)
```

### Selection 도메인
```java
@Operation(
    summary = "셀렉션 저장",
    description = """
    셀렉션 상세 페이지의 "저장" 버튼을 클릭해 셀렉션을 저장한다.
    
    **사용 페이지:**
    - 셀렉션 목록: 하트 아이콘 클릭
    - 셀렉션 상세: "저장" 버튼 클릭
    
    **플로우:**
    1. 셀렉션 상세 페이지 → "저장" 버튼 클릭
    2. PUT /selections/{selectionId}/save 호출
    3. 저장 상태 갱신 → "저장됨" UI 표시
    4. 마이 > 저장한 셀렉션에서 조회 가능
    
    같은 항목 저장 요청은 멱등적으로 처리되어 중복 저장이 방지된다.
    """
)
```

---

## 주의사항

### ❌ 하지 말 것
- description에 응답 필드 상세 설명 (DTO에 작성)
- 기술 구현 세부사항 (트랜잭션, 캐싱 등)
- 너무 긴 예제 (핵심만 포함)

### ✓ 해야 할 것
- 기능 + 사용 페이지 + 플로우를 명확히 구분
- 에러 발생 **조건**을 구체적으로 설명
- 스웨거 UI에서 "이 API는 언제 쓰는 거지?"라는 질문에 답변

---

## 스웨거 UI에서 확인

`http://localhost:8080/swagger-ui.html`에 접속해 다음을 확인:

1. **Tag 별 그룹화**: Category, Selection, Community 등
2. **Operation 요약**: 각 메서드의 기능이 한 줄로 표시
3. **설명 클릭**: 기능 + 페이지 + 플로우 정보 확인
4. **예제 확인**: 실제 요청/응답 형태 확인
5. **에러 조건**: 각 에러가 어떨 때 발생하는지 명시

---

## 참고

- **Sample**: `InterestControllerApiSpec` (기본 패턴)
- **Example**: `CategoryControllerApiSpec`, `CategoryPresetControllerApiSpec` (완전한 예시)
