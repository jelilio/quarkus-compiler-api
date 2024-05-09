package io.github.jelilio.model;

import java.util.List;
import java.util.Map;

public record Output (
    String sessionFilename,
    Map<String, OutputCode> languageOutput
) {
}
