package com.startup.domain.scenario.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioAssetUrlResolverTest {

    @Test
    void resolvesAssetKeyWithPublicBaseUrl() {
        ScenarioAssetUrlResolver resolver = new ScenarioAssetUrlResolver(
                "https://clueroom-assets.example.com/");

        String imageUrl = resolver.resolve("official/seowolchae/v1/evidence/EVIDENCE_TEST.png");

        assertThat(imageUrl).isEqualTo(
                "https://clueroom-assets.example.com/official/seowolchae/v1/evidence/EVIDENCE_TEST.png");
    }

    @Test
    void keepsExistingImageUrl() {
        ScenarioAssetUrlResolver resolver = new ScenarioAssetUrlResolver(
                "https://clueroom-assets.example.com");

        String imageUrl = resolver.resolve(
                "https://cdn.example.com/image.png",
                "official/seowolchae/v1/evidence/EVIDENCE_TEST.png");

        assertThat(imageUrl).isEqualTo("https://cdn.example.com/image.png");
    }

    @Test
    void returnsNullWhenBaseUrlIsBlank() {
        ScenarioAssetUrlResolver resolver = new ScenarioAssetUrlResolver("");

        String imageUrl = resolver.resolve("official/seowolchae/v1/evidence/EVIDENCE_TEST.png");

        assertThat(imageUrl).isNull();
    }

    @Test
    void returnsAbsoluteAssetKeyAsIs() {
        ScenarioAssetUrlResolver resolver = new ScenarioAssetUrlResolver(
                "https://clueroom-assets.example.com");

        String imageUrl = resolver.resolve("https://cdn.example.com/image.png");

        assertThat(imageUrl).isEqualTo("https://cdn.example.com/image.png");
    }

    @Test
    void trimsDuplicateSlashesBetweenBaseUrlAndAssetKey() {
        ScenarioAssetUrlResolver resolver = new ScenarioAssetUrlResolver(
                "https://clueroom-assets.example.com///");

        String imageUrl = resolver.resolve("///official/studio9/v1/scenario/SCENARIO_STUDIO9.cover.png");

        assertThat(imageUrl).isEqualTo(
                "https://clueroom-assets.example.com/official/studio9/v1/scenario/SCENARIO_STUDIO9.cover.png");
    }
}
