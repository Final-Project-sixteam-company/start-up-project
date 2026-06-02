package com.startup.domain.scenario.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScenarioAssetUrlResolver {

    private final String publicBaseUrl;

    public ScenarioAssetUrlResolver(@Value("${clueroom.assets.public-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    public String resolve(String assetKey) {
        if (!hasText(assetKey)) {
            return null;
        }

        String normalizedKey = normalizeAssetKey(assetKey);
        if (isAbsoluteUrl(normalizedKey)) {
            return normalizedKey;
        }
        if (!hasText(publicBaseUrl)) {
            return null;
        }

        return publicBaseUrl + "/" + normalizedKey;
    }

    public String resolve(String existingUrl, String assetKey) {
        if (hasText(existingUrl)) {
            return existingUrl.trim();
        }
        return resolve(assetKey);
    }

    private static String normalizeBaseUrl(String value) {
        if (!hasText(value)) {
            return null;
        }

        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeAssetKey(String value) {
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static boolean isAbsoluteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
