package com.guoyu.platform.trainingdepartment.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class TrainingDocxVariableReplacerTest {

  private static final Path TEMPLATE_WITH_WORKTYPE = Path.of(
      "../../storage/files/1/2026/06",
      "2b130ef9-2472-48b4-a5cd-e2485eb56a73-3978130658269af8f436a614.docx");

  @Test
  @EnabledIf("templateExists")
  void replacesWorktypeAndLevel() throws Exception {
    byte[] template = Files.readAllBytes(TEMPLATE_WITH_WORKTYPE.toAbsolutePath().normalize());
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("worktype", "电工");
    vars.put("wktype", "电工");
    vars.put("level", "五级/初级工");

    byte[] out = TrainingDocxVariableReplacer.replaceAll(template, vars);
    String xml = TrainingDocxTemplateValidator.extractWordXml(out);

    assertThat(xml).doesNotContain("${worktype}");
    assertThat(xml).doesNotContain("${level}");
    assertThat(xml).contains("电工");
    assertThat(xml).contains("五级/初级工");
  }

  static boolean templateExists() {
    return Files.isRegularFile(TEMPLATE_WITH_WORKTYPE.toAbsolutePath().normalize());
  }
}
