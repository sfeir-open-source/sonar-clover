/*
 * Sonar Clover Plugin
 * Copyright (C) 2008 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.clover;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CloverSensorTest {
  private Settings settings;
  private CloverSensor sensor;
  private Project project;
  private SensorContext context;
  private DefaultFileSystem fs;

  @Before
  public void setUp() throws Exception {
    settings = new Settings();
    fs = new DefaultFileSystem(new File("src/test/resources"));
    sensor = new CloverSensor(settings, fs, new PathResolver());
    context = mock(SensorContext.class);
    project = mock(Project.class);
  }

  @Test
  public void should_not_execute_if_report_path_empty() throws Exception {
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, "");
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_execute_if_report_path_set() throws Exception {
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, "clover/clover.xml");
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
    fs.add(new DefaultInputFile(cloverFilePath));
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, cloverFilePath);
    sensor.analyse(project, context);

  }
}
