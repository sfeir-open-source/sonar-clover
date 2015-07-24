/*
 * Clover :: Integration Tests
 * Copyright (C) 2009 SonarSource
 * dev@sonar.codehaus.org
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
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class CloverTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin("java")
      .addPlugin("clover")
      .addPlugin("groovy")
      .setMainPluginKey("clover")
      .build();

  public static String keyFor(String projectKey, String fileName, String srcDir) {
    return projectKey + srcDir + fileName;
  }

  @Test
  public void reuse_report_project_java() {
    String project = "reuseReport";
    String file = keyFor(project, "HelloWorld.java", ":src/main/java/");
    SonarRunner analysis = SonarRunner.create()
        .setProjectName(project)
        .setProjectKey(project)
        .setProjectVersion("1.0")
        .setSourceDirs("src/main/java")
        .setProjectDir(new File("projects/reuseReport"))
        .setProperty("sonar.clover.reportPath", "clover.xml");
    if(!orchestrator.getConfiguration().getPluginVersion("clover").isGreaterThan("2.9")) {
      analysis.setProperty("sonar.java.coveragePlugin", "clover");
    }
    orchestrator.executeBuild(analysis);
    assertThat(getMeasure(project, "lines_to_cover")).isEqualTo(4);
    assertThat(getMeasure(project, "uncovered_lines")).isEqualTo(2);

    assertThat(getMeasure(file, "files")).isEqualTo(1);
    assertThat(getMeasure(file, "lines_to_cover")).isEqualTo(4);
    assertThat(getMeasure(file, "uncovered_lines")).isEqualTo(2);
  }

  @Test
  public void reuse_report_project_groovy() {
    String project = "groovy-clover-sample";
    String groovyFile = keyFor(project, "org/sonar/Example.groovy", ":src/main/groovy/");

    SonarRunner analysis = SonarRunner.create()
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
    Resource resource = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKey));
    return resource != null ? resource.getMeasureIntValue(metricKey) : null;
  }

}
