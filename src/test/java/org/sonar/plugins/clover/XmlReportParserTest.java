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

import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Resource;
import org.sonar.api.test.IsMeasure;
import org.sonar.test.TestUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XmlReportParserTest {

  private XmlReportParser reportParser;
  private SensorContext context;
  private File xmlFile;

  @Before
  public void before() throws URISyntaxException {
    xmlFile = TestUtils.getResource(getClass(), "clover.xml");
    context = mock(SensorContext.class);
    FileProvider fp = mock(FileProvider.class);
    //Return a sonar resource file with the name corresponding to invocation
    when(fp.fromIOFile(anyString())).then(new Answer<org.sonar.api.resources.File>() {
      @Override
      public org.sonar.api.resources.File answer(InvocationOnMock invocationOnMock) throws Throwable {
        org.sonar.api.resources.File sonarFile = mock(org.sonar.api.resources.File.class);
        when(sonarFile.getName()).thenReturn((String) invocationOnMock.getArguments()[0]);
        return sonarFile;
      }
    });
    reportParser = new XmlReportParser(fp, context);

  }

  @Test
  public void parseClover232Format() throws ParseException, URISyntaxException {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_2_3_2.xml"));
    verify(context).saveMeasure(argThat(new SonarFileMatcher("ASTSensor.java")), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 68.0)));
    verify(context).saveMeasure(argThat(new SonarFileMatcher("ASTSensor.java")), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 6.0)));
  }

  @Test
  public void parse_clover_3_2_2_Format() throws ParseException, URISyntaxException {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_3_2_2.xml"));
    verify(context).saveMeasure(argThat(new SonarFileMatcher("SampleClass.java")), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 8.0)));
    verify(context).saveMeasure(argThat(new SonarFileMatcher("SampleClass.java")), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 3.0)));
  }

  @Test
  public void collectFileMeasures() throws Exception {
    reportParser.collect(xmlFile);
    verify(context).saveMeasure(argThat(new SonarFileMatcher("ClassUnderTest.java")), argThat(new IsMeasure(CoreMetrics.LINES_TO_COVER, 5.0)));
    verify(context).saveMeasure(argThat(new SonarFileMatcher("ClassUnderTest.java")), argThat(new IsMeasure(CoreMetrics.UNCOVERED_LINES, 0.0)));
    verify(context).saveMeasure(argThat(new SonarFileMatcher("ClassUnderTest.java")), argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "4=1;5=1;6=2;8=1;9=1")));
  }

  @Test
  public void collectFileHitsData() throws Exception {
    reportParser.collect(xmlFile);
    verify(context).saveMeasure(argThat(new SonarFileMatcher("ClassUnderTest.java")), argThat(new IsMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "4=1;5=1;6=2;8=1;9=1")));
  }

  @Test
  public void coverageShouldBeZeroWhenNoElements() throws URISyntaxException {
    File xmlFile = TestUtils.getResource(getClass(), "coverageShouldBeZeroWhenNoElements/clover.xml");
    reportParser.collect(xmlFile);
    verify(context, never()).saveMeasure((Resource) anyObject(), eq(CoreMetrics.COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) anyObject(), eq(CoreMetrics.LINE_COVERAGE), anyDouble());
    verify(context, never()).saveMeasure((Resource) anyObject(), eq(CoreMetrics.BRANCH_COVERAGE), anyDouble());
  }


  private class SonarFileMatcher extends BaseMatcher<org.sonar.api.resources.File> {

    private String filename;
    private String invokedFileName;

    private SonarFileMatcher(String filename) {
      this.filename = filename;
    }

    @Override
    public boolean matches(Object o) {
      if (o instanceof org.sonar.api.resources.File) {
        org.sonar.api.resources.File file = (org.sonar.api.resources.File) o;
        invokedFileName = extractFileName(file.getName());
        return filename.equals(invokedFileName);
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(invokedFileName).appendText(" instead of ").appendText(filename);
    }

    private String extractFileName(String filename) {
      if (filename != null) {
        filename = StringUtils.replaceChars(filename, '\\', '/');
        if (filename.indexOf('/') >= 0) {
          filename = StringUtils.substringAfterLast(filename, "/");
        }
      }
      return filename;
    }
  }

}
