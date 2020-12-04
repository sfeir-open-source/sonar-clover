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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Optional;

public class CloverSensor implements Sensor {

  static final String REPORT_PATH_PROPERTY = "sonar.clover.reportPath";
  static final String MISSING_FILE_MESSAGE = "Clover XML report not found";
  private final FileSystem fs;
  private final PathResolver pathResolver;
  private final Configuration configuration;

  @SuppressWarnings("WeakerAccess") // brings compatibility with sonarQube v 6.x
  public CloverSensor(Configuration configuration, FileSystem fs, PathResolver pathResolver) {
    this.configuration = configuration;
    this.fs = fs;
    this.pathResolver = pathResolver;
  }

  private File getReportFromProperty() {
    Optional<String> path = configuration.get(REPORT_PATH_PROPERTY);
    if (path.isPresent() && StringUtils.isNotEmpty(path.get())) {
      return pathResolver.relativeFile(fs.baseDir(), path.get());
    }

    return null;
  }

  private static boolean reportExists(@Nullable File report) {
    return report != null && report.isFile();
  }
  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Clover Coverage Analysis");
    descriptor.onlyOnLanguages("java", "grvy");
  }

  @Override
  public void execute(SensorContext context) {
    final File report = getReportFromProperty();
    if (reportExists(report)) {
      new CloverXmlReportParser(context, new InputFileProvider(fs)).collect(report);
    } else {
      Loggers.get(getClass()).warn(MISSING_FILE_MESSAGE);
    }
  }

}
