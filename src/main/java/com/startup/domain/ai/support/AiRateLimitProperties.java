package com.startup.domain.ai.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clueroom.ai.rate-limit")
public class AiRateLimitProperties {

    private boolean enabled = true;
    private long dailyLimitPerUserPerScenario = 150;
    private long dailyLimitPerUserTotal = 350;
    private long ttlHours = 24;
    private boolean adminBypassEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDailyLimitPerUserPerScenario() {
        return dailyLimitPerUserPerScenario;
    }

    public void setDailyLimitPerUserPerScenario(long dailyLimitPerUserPerScenario) {
        this.dailyLimitPerUserPerScenario = Math.max(0, dailyLimitPerUserPerScenario);
    }

    public long getDailyLimitPerUserTotal() {
        return dailyLimitPerUserTotal;
    }

    public void setDailyLimitPerUserTotal(long dailyLimitPerUserTotal) {
        this.dailyLimitPerUserTotal = Math.max(0, dailyLimitPerUserTotal);
    }

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = Math.max(1, ttlHours);
    }

    public boolean isAdminBypassEnabled() {
        return adminBypassEnabled;
    }

    public void setAdminBypassEnabled(boolean adminBypassEnabled) {
        this.adminBypassEnabled = adminBypassEnabled;
    }
}
