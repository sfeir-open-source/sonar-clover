/*
 * Sonar Clover Plugin
 * Copyright (C) 2008-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class CloverSensorTest {
  private SensorContextTester context = SensorContextTester.create(new File("src/test/resources/").getAbsoluteFile());

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void should_describe() throws Exception {
    final DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

    final CloverSensor sensor = new CloverSensor(new ConfigurationBridge(new MapSettings()), context.fileSystem(), new PathResolver());
    sensor.describe(descriptor);

    assertThat(descriptor.name()).isNotNull();
    assertThat(descriptor.languages()).hasSize(2);
  }


  @Test
  public void should_not_interact_if_no_report_path() throws Exception {
    final MapSettings settings = new MapSettings();
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, "");

    final Configuration configuration = new ConfigurationBridge(settings);
    final CloverSensor sensor = new CloverSensor(configuration, context.fileSystem(), new PathResolver());

    sensor.execute(context);

    assertThat(logTester.logs(LoggerLevel.WARN)).contains(
            CloverSensor.MISSING_FILE_MESSAGE);
  }

  @Test
  public void should_process_file() throws Exception {
    final String cloverFilePath = "org/sonar/plugins/clover/CloverXmlReportParserTest/clover_2_6_0.xml";
    final File cloverFile = TestUtils.getResource(cloverFilePath);
    final MapSettings settings = new MapSettings();
    settings.setProperty(CloverSensor.REPORT_PATH_PROPERTY, cloverFile.getAbsolutePath());

    final DefaultFileSystem fs = context.fileSystem();
    fs.add(new TestInputFileBuilder("", cloverFile.getAbsolutePath()).build());

    final Configuration configuration = new ConfigurationBridge(settings);
    final CloverSensor sensor = new CloverSensor(configuration, fs, new PathResolver());

    sensor.execute(context);

    assertThat(logTester.logs(LoggerLevel.INFO)).contains("Parsing " + cloverFile.getCanonicalPath());
    assertThat(logTester.logs(LoggerLevel.WARN).stream().anyMatch(s -> s.contains("14 files in Clover report did not match any file in SonarQube Index"))).isEqualTo(true);
  }
}
