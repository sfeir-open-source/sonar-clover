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
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CloverSensorTest {
  private Settings settings;
  private CloverSensor sensor;
  private DefaultFileSystem fs;
  private Project project;
  private SensorContext context;
  private CloverXmlReportParserFactory factory;
  private CloverXmlReportParser parser;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    fs = new DefaultFileSystem();
    factory = mock(CloverXmlReportParserFactory.class);
    parser = mock(CloverXmlReportParser.class);
    when(factory.create(any(Project.class), any(SensorContext.class))).thenReturn(parser);
    sensor = new CloverSensor(settings, fs, factory);
    context = mock(SensorContext.class);
    project = mock(Project.class);
  }

  @Test
  public void should_not_execute_on_static_analysis() throws Exception {
    settings.setProperty("sonar.java.coveragePlugin", CloverSensor.PLUGIN_KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.STATIC);
    fs.add(new DefaultInputFile("MyClass.java").setLanguage("java"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_if_no_java_file() throws Exception {
    settings.setProperty("sonar.java.coveragePlugin", CloverSensor.PLUGIN_KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_if_coverage_not_plugin_key() throws Exception {
    settings.setProperty("sonar.java.coveragePlugin", "cobertura");
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    fs.add(new DefaultInputFile("MyClass.java").setLanguage("java"));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_execute_if_clover_dynamic_analysis_and_java_files() throws Exception {
    settings.setProperty("sonar.java.coveragePlugin", CloverSensor.PLUGIN_KEY);
    when(project.getAnalysisType()).thenReturn(Project.AnalysisType.DYNAMIC);
    fs.add(new DefaultInputFile("MyClass.java").setLanguage("java"));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_not_interact_if_no_report_path() throws Exception {
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, "");
    sensor.analyse(project, context);
    verifyZeroInteractions(context);
  }

  @Test
  public void should_save_mesures() throws Exception {
    String cloverFilePath = "org/sonar/plugins/clover/CloverXmlReportParserTest/clover.xml";
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, cloverFilePath);
    ProjectFileSystem pfs = mock(ProjectFileSystem.class);
    File cloverXml = new File(getClass().getResource("/" + cloverFilePath).toURI());
    when(pfs.resolvePath(cloverFilePath)).thenReturn(cloverXml);
    when(project.getFileSystem()).thenReturn(pfs);

    sensor.analyse(project, context);
    verify(parser).collect(cloverXml);
  }
}
