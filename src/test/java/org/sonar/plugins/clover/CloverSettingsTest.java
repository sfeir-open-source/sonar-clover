/*
 * Sonar Clover Plugin
 * Copyright (C) 2008 SonarSource
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
package org.sonar.plugins.clover;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloverSettingsTest {

  Settings settings;
  CloverSettings cloverSettings;
  Project javaProject;

  @Before
  public void init() {
    settings = new Settings();
    settings.setProperty("sonar.java.coveragePlugin", "clover");
    cloverSettings = new CloverSettings(settings);
    ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.mainFiles(Java.KEY)).thenReturn(Arrays.asList(InputFileUtils.create(null, "")));
    javaProject = mock(Project.class);
    when(javaProject.getLanguageKey()).thenReturn(Java.KEY);
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    when(javaProject.getFileSystem()).thenReturn(fileSystem);
  }

  @Test
  public void should_support_deprecated_property() {
    // before sonar 3.4
    settings.setProperty("sonar.core.codeCoveragePlugin", "clover,phpunit");
    assertThat(cloverSettings.isEnabled(javaProject)).isTrue();

    settings.setProperty("sonar.core.codeCoveragePlugin", "cobertura");
    assertThat(cloverSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_support_sonar_3_4_property() {
    // since sonar 3.4
    settings.setProperty("sonar.java.coveragePlugin", "clover");
    assertThat(cloverSettings.isEnabled(javaProject)).isTrue();

    settings.setProperty("sonar.java.coveragePlugin", "jacoco");
    assertThat(cloverSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_be_disabled_if_static_analysis() {
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    assertThat(cloverSettings.isEnabled(javaProject)).isFalse();
  }

  @Test
  public void should_be_enabled_if_reuse_report_mode() {
    when(javaProject.getAnalysisType()).thenReturn(Project.AnalysisType.REUSE_REPORTS);
    assertThat(cloverSettings.isEnabled(javaProject)).isTrue();
  }

  @Test
  public void should_change_report_path() {
    assertThat(cloverSettings.getReportPath()).isNull();
    cloverSettings.setReportPath("path/to/report");
    assertThat(cloverSettings.getReportPath()).isEqualTo("path/to/report");
  }

  @Test
  public void should_get_report_path() {
    settings.setProperty(CloverConstants.REPORT_PATH_PROPERTY, "path/to/report");
    assertThat(cloverSettings.getReportPath()).isEqualTo("path/to/report");
  }
}
