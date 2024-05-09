package io.github.jelilio.model;

import java.util.List;
import java.util.Map;

public record OutputCode(
    boolean isError,
    List<String> outputs
) {
}
