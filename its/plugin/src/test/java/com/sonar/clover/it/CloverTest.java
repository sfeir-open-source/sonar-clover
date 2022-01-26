/*
 * Clover :: Integration Tests
 * Copyright (C) 2009 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.clover.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class CloverTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion("9.1.0")
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    .setOrchestratorProperty("groovyVersion", "LATEST_RELEASE")
    .addPlugin("grvy")
    .addPlugin(FileLocation.of("../../target/sonar-clover-plugin.jar"))
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile.xml"))
    .build();

  public static String keyFor(String projectKey, String fileName, String srcDir) {
    return projectKey + srcDir + fileName;
  }

  @Test
  public void reuse_report_project_java() {
    String project = "reuseReport";
    String file = keyFor(project, "HelloWorld.java", ":src/main/java/");
    SonarScanner analysis = SonarScanner.create()
      .setProjectName(project)
      .setProjectKey(project)
      .setProjectVersion("1.0")
      .setSourceDirs("src/main/java")
      .setProjectDir(new File("projects/reuseReport"))
      .setProperty("sonar.clover.reportPath", "clover.xml")
      .setProperty("sonar.java.binaries", "target/classes");
    if (!orchestrator.getConfiguration().getPluginVersion("clover").isGreaterThan("2.9")) {
      analysis.setProperty("sonar.java.coveragePlugin", "clover");
    }

    orchestrator.executeBuild(analysis);

    assertThat(getMeasure(project, "lines_to_cover")).isEqualTo(8);
    assertThat(getMeasure(project, "uncovered_lines")).isEqualTo(4);

    assertThat(getMeasure(file, "files")).isEqualTo(1);
    assertThat(getMeasure(file, "lines_to_cover")).isEqualTo(4);
    assertThat(getMeasure(file, "uncovered_lines")).isEqualTo(2);
  }

  @Test
  public void reuse_report_project_groovy() {
    String project = "groovy-clover-sample";
    String groovyFile = keyFor(project, "org/sonar/Example.groovy", ":src/main/groovy/");
    orchestrator.getServer().provisionProject("groovy-clover-sample", "groovy-clover-sample");
    orchestrator.getServer().associateProjectToQualityProfile("groovy-clover-sample", "grvy", "rules");
    SonarScanner analysis = SonarScanner.create()
      .setProjectName(project)
      .setProjectKey(project)
      .setProjectVersion("1.0")
      .setSourceDirs("src/main/groovy")
      .setLanguage("grvy")
      .setProjectDir(new File("projects/groovy-clover-sample"))
      .setProperty("sonar.clover.reportPath", "clover.xml");

    orchestrator.executeBuild(analysis);

    assertThat(getMeasure(project, "lines_to_cover")).isEqualTo(2);
    assertThat(getMeasure(project, "uncovered_lines")).isEqualTo(0);

    assertThat(getMeasure(groovyFile, "files")).isEqualTo(1);
    assertThat(getMeasure(groovyFile, "lines_to_cover")).isEqualTo(2);
    assertThat(getMeasure(groovyFile, "uncovered_lines")).isEqualTo(0);
  }

  private Integer getMeasure(String resourceKey, String metricKey) {
    final String body = orchestrator.getServer().newHttpCall("/api/measures/component").setParam("component", resourceKey).setParam("metricKeys", metricKey).execute().getBodyAsString();

    try {
      final JSONObject componentJson = (JSONObject) ((JSONObject) new JSONParser().parse(body)).get("component");
      String result = (String) ((JSONObject) ((JSONArray) componentJson.get("measures")).get(0)).get("value");
      return result == null ? null : Integer.valueOf(result);
    } catch (ParseException pe) {
      return null;
    }
  }
}
