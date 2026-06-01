package com.startup.domain.play.support;

import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceVariantState;
import com.startup.domain.scenario.repository.EvidenceVariantStateRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvidenceVariantDescriptionResolverTest {

    @Test
    void resolve_appendsVariantSafeDetail() {
        EvidenceVariantStateRepository repository = mock(EvidenceVariantStateRepository.class);
        EvidenceVariantDescriptionResolver resolver = new EvidenceVariantDescriptionResolver(repository);
        Evidence evidence = Evidence.builder()
                .scenarioId(1L)
                .code("EVIDENCE_KEY")
                .title("핵심 증거")
                .description("기본 설명")
                .sortOrder(1)
                .build();

        when(repository.findByVariantIdAndEvidenceId(eq(7L), isNull()))
                .thenReturn(Optional.of(EvidenceVariantState.builder()
                        .scenarioId(1L)
                        .variantId(7L)
                        .evidenceId(null)
                        .variantCode("VARIANT_SECRETARY")
                        .evidenceCode("EVIDENCE_KEY")
                        .role("METHOD_KEY")
                        .detailAppend("Variant별 설명")
                        .build()));

        String description = resolver.resolve(evidence, 7L);

        assertThat(description).isEqualTo("기본 설명\n\nVariant별 설명");
    }

    @Test
    void resolve_prefersOverrideOverAppend() {
        EvidenceVariantStateRepository repository = mock(EvidenceVariantStateRepository.class);
        EvidenceVariantDescriptionResolver resolver = new EvidenceVariantDescriptionResolver(repository);
        Evidence evidence = Evidence.builder()
                .scenarioId(1L)
                .code("EVIDENCE_KEY")
                .title("핵심 증거")
                .description("기본 설명")
                .sortOrder(1)
                .build();

        when(repository.findByVariantIdAndEvidenceId(eq(7L), isNull()))
                .thenReturn(Optional.of(EvidenceVariantState.builder()
                        .scenarioId(1L)
                        .variantId(7L)
                        .evidenceId(null)
                        .variantCode("VARIANT_SECRETARY")
                        .evidenceCode("EVIDENCE_KEY")
                        .role("METHOD_KEY")
                        .detailOverride("대체 설명")
                        .detailAppend("무시할 설명")
                        .build()));

        String description = resolver.resolve(evidence, 7L);

        assertThat(description).isEqualTo("대체 설명");
    }
}
