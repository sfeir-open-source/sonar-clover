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

public interface CloverConstants {

  String LICENSE_PROPERTY = "sonar.clover.license.secured";
  String VERSION_PROPERTY = "sonar.clover.version";
  String REPORT_PATH_PROPERTY = "sonar.clover.reportPath";
  
  String MAVEN_GROUP_ID = "com.atlassian.maven.plugins";
  String MAVEN_ARTIFACT_ID = "maven-clover2-plugin";
  String MAVEN_DEFAULT_VERSION = "3.0.5";
  String PLUGIN_KEY = "clover";
}
