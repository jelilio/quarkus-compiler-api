package io.github.jelilio.config;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Map;

@ConfigMapping(prefix = "compiler")
public interface CompilerConfig {
  String directory();
  String outputExt();
  Map<String, String> languageExt();
  Map<String, List<String>> languages();
}
