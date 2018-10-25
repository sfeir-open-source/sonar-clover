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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.XmlParserException;
import org.sonar.test.TestUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class CloverXmlReportParserTest {

  private CloverXmlReportParser reportParser;
  private SensorContext context;
  private File xmlFile;
  private InputFileProvider provider;

  @Before
  public void before() throws URISyntaxException {
    xmlFile = TestUtils.getResource(getClass(), "clover.xml");
    context = mock(SensorContext.class);
    //Return a sonar resource file with the name corresponding to invocation
    provider = new InputFileProvider(null) {
      @Override
      public InputFile fromPath(String path) {
        DefaultInputFile inputFile = new DefaultInputFile(path);
        inputFile.setAbsolutePath(path);
        return inputFile;
      }
    };
    reportParser = new CloverXmlReportParser(context, provider);

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

  @Test(expected = XmlParserException.class)
  public void bad_clover_should_throw_exception() throws Exception {
    reportParser.collect(TestUtils.getResource(getClass(), "bad_clover.xml"));
  }

  private class SonarFileMatcher extends BaseMatcher<InputFile> {

    private String filename;
    private String invokedFileName;

    private SonarFileMatcher(String filename) {
      this.filename = filename;
    }

    @Override
    public boolean matches(Object o) {
      if (o instanceof InputFile) {
        InputFile file = (InputFile) o;
        invokedFileName = extractFileName(file.absolutePath());
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

  public class IsMeasure extends BaseMatcher<Measure> {
    private Metric metric = null;
    private Double value = null;
    private String data = null;
    private String mismatchTxt;

    public IsMeasure(Metric metric, Double value) {
      this.metric = metric;
      this.value = value;
    }

    public IsMeasure(Metric metric, String data) {
      this.metric = metric;
      this.data = data;
    }

    public boolean matches(Object o) {
      Measure m = (Measure) o;
      if (this.metric != null && !ObjectUtils.equals(this.metric, m.getMetric())) {
        this.mismatchTxt = "metric: " + this.metric.getKey();
        return false;
      } else if (this.value != null && NumberUtils.compare(this.value.doubleValue(), m.getValue().doubleValue()) != 0) {
        this.mismatchTxt = "value: " + this.value;
        return false;
      } else if (this.data != null && !ObjectUtils.equals(this.data, m.getData())) {
        this.mismatchTxt = "data: " + this.data;
        return false;
      } else {
        return true;
      }
    }

    public void describeTo(Description description) {
      description.appendText(this.mismatchTxt);
    }
  }
}
