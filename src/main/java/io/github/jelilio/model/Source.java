package io.github.jelilio.model;

import java.util.List;

public record Source(
    String language,
    String version,
    List<SourceCode> files
) {
}
