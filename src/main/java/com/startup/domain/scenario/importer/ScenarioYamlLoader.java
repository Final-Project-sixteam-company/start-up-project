package com.startup.domain.scenario.importer;

import com.startup.domain.scenario.importer.yaml.ScenarioYaml;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ScenarioYamlLoader {

    private final YAMLMapper yamlMapper;

    public ScenarioYamlLoader() {
        this.yamlMapper = YAMLMapper.builder().build();
    }

    public ScenarioYaml load(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return yamlMapper.readValue(inputStream, ScenarioYaml.class);
        } catch (IOException e) {
            throw new ScenarioImportException("시나리오 YAML 파일을 읽을 수 없습니다: " + resource.getDescription(), e);
        }
    }

    public ScenarioYaml load(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return yamlMapper.readValue(inputStream, ScenarioYaml.class);
        } catch (IOException e) {
            throw new ScenarioImportException("시나리오 YAML 파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
