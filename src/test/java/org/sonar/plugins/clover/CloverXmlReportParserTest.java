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

import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.XmlParserException;

import java.io.File;
import java.net.URISyntaxException;

import static org.fest.assertions.Assertions.assertThat;

public class CloverXmlReportParserTest {

  private CloverXmlReportParser reportParser;
  private SensorContextTester context = SensorContextTester.create(new File("src/test/resources/"));
  private InputFileProvider provider;

  @Before
  public void before() {
    // Return a sonar resource file with the name corresponding to invocation
    provider = new InputFileProvider(null) {
      @Override
      public InputFile fromPath(String path) {
        return new TestInputFileBuilder("", path).setLines(1_000).build();
      }
    };

    reportParser = new CloverXmlReportParser(context, provider);
  }

  @Test
  public void parseClover_2_3_2_Format() {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_2_3_2.xml"));

    final String testFileName = ":/Users/cmunger/dev/workspace/sonar/sonar-squid/src/main/java/org/sonar/squid/sensors/ASTSensor.java";
    assertThat(context.lineHits(testFileName, 44)).isEqualTo(1);
    assertThat(context.conditions(testFileName, 157)).isEqualTo(2);
    assertThat(context.coveredConditions(testFileName, 157)).isEqualTo(2);
  }

  @Test
  public void parse_clover_3_2_2_Format() {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_3_2_2.xml"));

    final String testFileName = ":/home/benzonico/Development/SonarSource/clover-sample/src/main/java/SampleClass.java";
    assertThat(context.lineHits(testFileName, 6)).isEqualTo(1);
    assertThat(context.conditions(testFileName, 6)).isEqualTo(2);
    assertThat(context.coveredConditions(testFileName, 6)).isEqualTo(1);
  }

  @Test
  public void parse_clover_4_1_1_Format() {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_4_1_1.xml"));

    final String testFileName = ":/clover-examples/parameterized-junit4-example/src/test/java/Square.java";
    assertThat(context.lineHits(testFileName, 6)).isEqualTo(12);
    assertThat(context.conditions(testFileName, 6)).isNull();
    assertThat(context.coveredConditions(testFileName, 6)).isNull();

    final String omittedFileName = ":/clover-examples/parameterized-junit4-example/src/test/java/Omit.java";
    assertThat(context.lineHits(omittedFileName, 6)).isNull();
    assertThat(context.conditions(omittedFileName, 6)).isNull();
    assertThat(context.coveredConditions(omittedFileName, 6)).isNull();
  }

  @Test
  public void parse_clover_2_6_0_Format() {
    reportParser.collect(TestUtils.getResource(getClass(), "clover_2_6_0.xml"));

    final String testFileName = ":/Users/simon/projects/sonar/trunk/tests/integration/reference-projects/reference/src/main/java/org/sonar/samples/ClassUnderTest.java";
    assertThat(context.lineHits(testFileName, 4)).isEqualTo(1);
    assertThat(context.conditions(testFileName, 9)).isNull();
    assertThat(context.coveredConditions(testFileName, 9)).isNull();
  }

  @Test
  public void coverageShouldBeZeroWhenNoElements() throws URISyntaxException {
    File xmlFile = TestUtils.getResource(getClass(), "coverageShouldBeZeroWhenNoElements/clover.xml");
    reportParser.collect(xmlFile);

    final String testFileName = ":/sonar/sonar-commons/src/main/java/ch/hortis/sonar/model/MetricsClassType.java";
    assertThat(context.lineHits(testFileName, 1)).isNull();
    assertThat(context.conditions(testFileName, 1)).isNull();
    assertThat(context.coveredConditions(testFileName, 1)).isNull();
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
        invokedFileName = extractFileName(file.filename());
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
