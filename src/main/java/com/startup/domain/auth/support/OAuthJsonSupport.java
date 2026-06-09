package com.startup.domain.auth.support;

import tools.jackson.databind.JsonNode;

final class OAuthJsonSupport {

    private OAuthJsonSupport() {
    }

    static String textAt(JsonNode root, String path) {
        JsonNode node = root;
        for (String segment : path.split("\\.")) {
            if (node == null || node.isNull()) {
                return null;
            }
            node = node.get(segment);
        }
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    static boolean booleanAt(JsonNode root, String path) {
        JsonNode node = root;
        for (String segment : path.split("\\.")) {
            if (node == null || node.isNull()) {
                return false;
            }
            node = node.get(segment);
        }
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return "true".equalsIgnoreCase(node.asText());
    }
}
