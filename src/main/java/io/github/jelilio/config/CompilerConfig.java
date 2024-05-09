package io.github.jelilio.config;

import io.smallrye.config.ConfigMapping;

import java.util.List;
import java.util.Map;

@ConfigMapping(prefix = "compiler")
public interface CompilerConfig {
  String directory();
  List<String> othersExt();
  Map<String, String> languageExt();
  Map<String, String> languageOutExt();
  Map<String, String> languageOutErrExt();
  Map<String, List<String>> languages();
}
