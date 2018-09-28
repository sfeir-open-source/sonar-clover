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

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.PathResolver;

import javax.annotation.Nullable;

import java.io.File;

public class CloverSensor implements Sensor, CoverageExtension {
  public static final String REPORT_PATH_PROPERTY = "sonar.clover.reportPath";
  private final FileSystem fs;
  private final PathResolver pathResolver;
  private Settings settings;

  public CloverSensor(Settings settings, FileSystem fs, PathResolver pathResolver) {
    this.settings = settings;
    this.fs = fs;
    this.pathResolver = pathResolver;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return StringUtils.isNotEmpty(settings.getString(REPORT_PATH_PROPERTY));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    File report = getReportFromProperty();
    if (reportExists(report)) {
      new CloverXmlReportParser(context, new InputFileProvider(fs)).collect(report);
    } else {
      LoggerFactory.getLogger(getClass()).warn("Clover XML report not found");
    }
  }

  private File getReportFromProperty() {
    String path = settings.getString(REPORT_PATH_PROPERTY);
    if (StringUtils.isNotEmpty(path)) {
      return pathResolver.relativeFile(fs.baseDir(), path);
    }
    return null;
  }

  private static boolean reportExists(@Nullable File report) {
    return report != null && report.isFile();
  }

}
