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

import org.apache.commons.lang.ArrayUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

public class CloverSettings implements BatchExtension {

  private Settings settings;

  public CloverSettings(Settings settings) {
    this.settings = settings;
  }

  public boolean isEnabled(Project project) {
    if (project.getAnalysisType().isDynamic(true) && !project.getFileSystem().mainFiles(Java.KEY).isEmpty()) {
      // backward-compatibility with the property that has been deprecated in sonar 3.4.
      String[] keys = settings.getStringArray("sonar.core.codeCoveragePlugin");
      if (keys.length > 0) {
        return ArrayUtils.contains(keys, CloverConstants.PLUGIN_KEY);
      }

      // should use org.sonar.plugins.java.api.JavaSettings#getEnabledCoveragePlugin() introduced in sonar-java-plugin 1.1 with sonar 3.4
      return CloverConstants.PLUGIN_KEY.equals(settings.getString("sonar.java.coveragePlugin"));
    }
    return false;
  }

  public String getReportPath() {
    return settings.getString(CloverConstants.REPORT_PATH_PROPERTY);
  }

  public CloverSettings setReportPath(String s) {
    settings.setProperty(CloverConstants.REPORT_PATH_PROPERTY, s);
    return this;
  }
}
