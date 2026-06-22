package com.startup.domain.ai.support;

import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiTokenUsage;
import com.startup.domain.ai.enums.AiFeatureType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiCallLogWriterTest {

    @Test
    void write_disabled_doesNotRequestJdbcTemplate() {
        ObjectProvider<JdbcTemplate> provider = mockProvider();
        AiCallLogWriter writer = new AiCallLogWriter(provider, false);

        writer.write(context(), "deepseek", "deepseek-v4-flash", "npc_interrogation_v1",
                1234L, true, "none", false, new AiTokenUsage(10, 20, 30));

        verifyNoInteractions(provider);
    }

    @Test
    void write_enabled_persistsMetadataOnly() {
        ObjectProvider<JdbcTemplate> provider = mockProvider();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(provider.getIfAvailable()).thenReturn(jdbcTemplate);
        AiCallLogWriter writer = new AiCallLogWriter(provider, true);

        writer.write(context(), "deepseek", "deepseek-v4-flash", "npc_interrogation_v1",
                1234L, true, "none", false, new AiTokenUsage(10, 20, 30));

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(anyString(), argsCaptor.capture());

        Object[] args = argsCaptor.getValue();
        assertThat(args).containsExactly(
                "INTERROGATION",
                "deepseek",
                "deepseek-v4-flash",
                "npc_interrogation_v1",
                1L,
                2L,
                3L,
                "NPC_SECRETARY",
                1234L,
                true,
                "none",
                false,
                10,
                20,
                30
        );
    }

    private AiCallContext context() {
        return new AiCallContext(
                AiFeatureType.INTERROGATION,
                "npc_interrogation_v1",
                1L,
                2L,
                3L,
                "NPC_SECRETARY"
        );
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JdbcTemplate> mockProvider() {
        return mock(ObjectProvider.class);
    }
}
