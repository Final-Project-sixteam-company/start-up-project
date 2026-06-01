package com.startup.domain.scenario.importer;

import com.startup.domain.scenario.importer.yaml.ScenarioYaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clueroom.scenario-import", name = "enabled", havingValue = "true")
public class ScenarioYamlImportRunner implements ApplicationRunner {

    private final ScenarioYamlLoader loader;
    private final ScenarioYamlImportService importService;
    private final ResourceLoader resourceLoader;

    @Value("${clueroom.scenario-import.paths:}")
    private String paths;

    @Value("${clueroom.scenario-import.fail-on-error:true}")
    private boolean failOnError;

    @Override
    public void run(ApplicationArguments args) {
        List<String> importPaths = parsePaths(paths);
        if (importPaths.isEmpty()) {
            log.info("[ScenarioImport] enabled=true but paths is empty. Skip scenario import.");
            return;
        }

        for (String importPath : importPaths) {
            try {
                importPath(importPath);
            } catch (Exception e) {
                log.error("[ScenarioImport] failed: path={}", importPath, e);
                if (failOnError) {
                    throw e;
                }
            }
        }
    }

    private void importPath(String importPath) {
        if (importPath.startsWith("classpath:")) {
            importResource(importPath);
            return;
        }

        Path path = toPath(importPath);
        if (!Files.exists(path)) {
            throw new ScenarioImportException("시나리오 YAML 경로가 존재하지 않습니다: " + importPath);
        }
        if (Files.isDirectory(path)) {
            for (Path yamlFile : listYamlFiles(path)) {
                importFile(yamlFile);
            }
            return;
        }
        importFile(path);
    }

    private void importResource(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new ScenarioImportException("classpath 시나리오 YAML을 찾을 수 없습니다: " + location);
        }
        String hash = sha256(resource);
        ScenarioYaml yaml = loader.load(resource);
        ScenarioImportResult result = importService.importYaml(yaml, hash, location);
        log.info("[ScenarioImport] {} {}@{} source={}",
                result.status(), result.scenarioCode(), result.version(), location);
    }

    private void importFile(Path path) {
        String hash = sha256(path);
        ScenarioYaml yaml = loader.load(path);
        ScenarioImportResult result = importService.importYaml(yaml, hash, path.toString());
        log.info("[ScenarioImport] {} {}@{} source={}",
                result.status(), result.scenarioCode(), result.version(), path);
    }

    private List<Path> listYamlFiles(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
                    })
                    .sorted()
                    .toList();
        } catch (Exception e) {
            throw new ScenarioImportException("시나리오 YAML 디렉터리를 읽을 수 없습니다: " + directory, e);
        }
    }

    private Path toPath(String importPath) {
        if (importPath.startsWith("file:")) {
            return Path.of(URI.create(importPath));
        }
        return Path.of(importPath);
    }

    private String sha256(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return sha256(inputStream);
        } catch (Exception e) {
            throw new ScenarioImportException("시나리오 YAML hash 계산에 실패했습니다: " + path, e);
        }
    }

    private String sha256(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return sha256(inputStream);
        } catch (Exception e) {
            throw new ScenarioImportException("시나리오 YAML hash 계산에 실패했습니다: "
                    + resource.getDescription(), e);
        }
    }

    private String sha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new ScenarioImportException("시나리오 YAML hash 계산에 실패했습니다.", e);
        }
    }

    private List<String> parsePaths(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
