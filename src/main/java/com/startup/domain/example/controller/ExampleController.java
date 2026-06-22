package com.startup.domain.example.controller;

import com.startup.common.dto.ApiResponse;
import com.startup.domain.example.dto.ExampleCreateRequest;
import com.startup.domain.example.dto.ExampleResponse;
import com.startup.domain.example.dto.ExampleStatusUpdateRequest;
import com.startup.domain.example.dto.ExampleUpdateRequest;
import com.startup.domain.example.service.ExampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Example", description = "팀 컨벤션 확인용 CRUD 예제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/examples")
// 팀원이 Controller 응답 형식, validation, Swagger 문서화 방식을 맞춰 볼 수 있는 예제다.
public class ExampleController {

    private final ExampleService exampleService;

    @Operation(summary = "Example 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ExampleResponse>> createExample(
            @Valid @RequestBody ExampleCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(exampleService.createExample(request)));
    }

    @Operation(summary = "Example 목록 조회", description = "status가 없으면 전체를 최신 생성순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExampleResponse>>> getExamples(
            @Parameter(description = "DRAFT, ACTIVE, ARCHIVED 또는 한글 라벨")
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.getExamples(status)));
    }

    @Operation(summary = "Example 단건 조회")
    @GetMapping("/{exampleId}")
    public ResponseEntity<ApiResponse<ExampleResponse>> getExample(
            @PathVariable Long exampleId
    ) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.getExample(exampleId)));
    }

    @Operation(summary = "Example 수정")
    @PutMapping("/{exampleId}")
    public ResponseEntity<ApiResponse<ExampleResponse>> updateExample(
            @PathVariable Long exampleId,
            @Valid @RequestBody ExampleUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.updateExample(exampleId, request)));
    }

    @Operation(summary = "Example 상태 변경")
    @PatchMapping("/{exampleId}/status")
    public ResponseEntity<ApiResponse<ExampleResponse>> updateExampleStatus(
            @PathVariable Long exampleId,
            @Valid @RequestBody ExampleStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.updateExampleStatus(exampleId, request)));
    }

    @Operation(summary = "Example 삭제")
    @DeleteMapping("/{exampleId}")
    public ResponseEntity<ApiResponse<Void>> deleteExample(
            @PathVariable Long exampleId
    ) {
        exampleService.deleteExample(exampleId);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
