package com.startup.domain.scenario.importer;

import java.util.List;

public class ScenarioImportException extends RuntimeException {

    private final List<String> violations;

    public ScenarioImportException(String message) {
        super(message);
        this.violations = List.of(message);
    }

    public ScenarioImportException(String message, Throwable cause) {
        super(message, cause);
        this.violations = List.of(message);
    }

    public ScenarioImportException(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
