package com.startup.domain.ai.dto;

import com.startup.domain.ai.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "심문 요청")
public record InterrogationRequest(
        @Schema(description = "용의자 ID", example = "1")
        @NotNull(message = "용의자 ID는 필수입니다.")
        Long suspectId,

        @Schema(description = "질문 유형", example = "FREE")
        @NotNull(message = "질문 유형은 필수입니다.")
        QuestionType questionType,

        @Schema(description = "사용자 질문", example = "사건 당시 어디에 있었습니까?")
        @NotBlank(message = "질문은 필수입니다.")
        @Size(max = 500, message = "질문은 500자 이하로 입력해 주세요.")
        String question,

        @Schema(description = "제시할 증거 ID (증거 제시 심문 시)", example = "3")
        Long presentedEvidenceId
) {
        // 증거 제시 심문(EVIDENCE_PRESENTED)인데 제시 증거 ID가 없으면 AI 호출 전에 400으로 차단한다.
        @AssertTrue(message = "증거 제시 심문에는 제시할 증거 ID가 필요합니다.")
        @Schema(hidden = true)
        public boolean isPresentedEvidenceProvided() {
                if (questionType != QuestionType.EVIDENCE_PRESENTED) {
                        return true;
                }
                return presentedEvidenceId != null;
        }

        // 반대 방향: 증거 제시 심문이 아니면(FREE/RECOMMENDED) 제시 증거 ID를 보내면 안 된다.
        @AssertTrue(message = "증거 제시 심문이 아니면 제시 증거 ID를 보낼 수 없습니다.")
        @Schema(hidden = true)
        public boolean isPresentedEvidenceAbsentForNonEvidenceQuestion() {
                if (questionType == QuestionType.EVIDENCE_PRESENTED) {
                        return true;
                }
                return presentedEvidenceId == null;
        }
}
