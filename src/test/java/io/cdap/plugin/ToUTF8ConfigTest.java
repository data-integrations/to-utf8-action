/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.plugin;

import io.cdap.cdap.etl.api.validation.CauseAttributes;
import io.cdap.cdap.etl.api.validation.ValidationException;
import io.cdap.cdap.etl.api.validation.ValidationFailure;
import io.cdap.cdap.etl.mock.action.MockActionContext;
import io.cdap.cdap.etl.mock.common.MockPipelineConfigurer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ToUTF8Action.ToUTF8Config}
 */
public class ToUTF8ConfigTest {
  private static final String ISO_8859_FILE_NAME = "20150320_clo_prod_cln.dat";
  private static final String UTF_8_FILE_NAME = "20150320_clo_prod_cln.dat.utf8";
  private static final String MOCK_NAME = "mockstage";
  private static final String STAGE = "stage";

  private FileFilter filter = new FileFilter() {
    private final Pattern pattern = Pattern.compile("[^\\.].*\\.utf8");

    @Override
    public boolean accept(File pathname) {
      return pattern.matcher(pathname.getName()).matches();
    }
  };

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testSingleFile() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    URL iso88591File = classLoader.getResource(ISO_8859_FILE_NAME);
    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config(iso88591File.getFile(),
                                                                     destFolder.getPath() + "/" + UTF_8_FILE_NAME,
                                                                     null, "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    MockActionContext context = new MockActionContext();
    new ToUTF8Action(config).run(context);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testFolder() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    URL iso88591File = classLoader.getResource(ISO_8859_FILE_NAME);

    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config(new File(iso88591File.getFile()).getParent(),
                                                                     destFolder.getPath(),
                                                                     null, "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    MockActionContext context = new MockActionContext();
    new ToUTF8Action(config).run(context);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testFolderWithGlob() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    URL iso88591File = classLoader.getResource(ISO_8859_FILE_NAME);

    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config =
      new ToUTF8Action.ToUTF8Config(new File(iso88591File.getFile()).getParent() + "/*.dat",
                                    destFolder.getPath(),
                                    null, "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    MockActionContext context = new MockActionContext();
    new ToUTF8Action(config).run(context);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testFolderWithRegEx() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    URL iso88591File = classLoader.getResource(ISO_8859_FILE_NAME);

    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config(new File(iso88591File.getFile()).getParent(),
                                                                     destFolder.getPath(),
                                                                     ".*\\.dat", "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    MockActionContext context = new MockActionContext();
    new ToUTF8Action(config).run(context);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testValidationEmptySourcePath() {
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config("", "",
                                                                     ".*\\.dat", "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    try {
      new ToUTF8Action(config).configurePipeline(configurer);
    } catch (ValidationException e) {
      assertEquals(1, e.getFailures().size());
      ValidationFailure validationFailure = e.getFailures().get(0);
      assertEquals("Source file or folder is required.", validationFailure.getMessage());
      assertEquals(1, validationFailure.getCauses().size());
      ValidationFailure.Cause cause = validationFailure.getCauses().get(0);
      assertEquals(ToUTF8Action.ToUTF8Config.SOURCE_FILE_PATH, cause.getAttribute(CauseAttributes.STAGE_CONFIG));
      assertEquals(MOCK_NAME, cause.getAttribute(STAGE));
    }
  }

  @Test
  public void testValidationEmptyDestinationPath() {
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config("_12_test", "",
                                                                     ".*\\.dat", "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    try {
      new ToUTF8Action(config).configurePipeline(configurer);
    } catch (ValidationException e) {
      assertEquals(1, e.getFailures().size());
      ValidationFailure validationFailure = e.getFailures().get(0);
      assertEquals("Destination file or folder is required.", validationFailure.getMessage());
      assertEquals(1, validationFailure.getCauses().size());
      ValidationFailure.Cause cause = validationFailure.getCauses().get(0);
      assertEquals(ToUTF8Action.ToUTF8Config.DEST_FILE_PATH, cause.getAttribute(CauseAttributes.STAGE_CONFIG));
      assertEquals(MOCK_NAME, cause.getAttribute(STAGE));
    }
  }

  @Test
  public void testValidationFileRegularExpression() {
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config("source_path", "dest_path",
                                                                     "*\\.dat", "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    try {
      new ToUTF8Action(config).configurePipeline(configurer);
    } catch (ValidationException e) {
      assertEquals(1, e.getFailures().size());
      ValidationFailure validationFailure = e.getFailures().get(0);
      assertEquals("The regular expression pattern provided is not a valid regular expression.",
                   validationFailure.getMessage());
      assertEquals(1, validationFailure.getCauses().size());
      ValidationFailure.Cause cause = validationFailure.getCauses().get(0);
      assertEquals(ToUTF8Action.ToUTF8Config.FILE_REGEX, cause.getAttribute(CauseAttributes.STAGE_CONFIG));
      assertEquals(MOCK_NAME, cause.getAttribute(STAGE));
    }
  }

  @Test
  public void testValidationCharacterSet() {
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config("source_path", "dest_path",
                                                                     ".*\\.dat", "ISO-885-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    try {
      new ToUTF8Action(config).configurePipeline(configurer);
    } catch (ValidationException e) {
      assertEquals(1, e.getFailures().size());
      ValidationFailure validationFailure = e.getFailures().get(0);
      assertEquals("ISO-885-1", validationFailure.getMessage());
      assertEquals(1, validationFailure.getCauses().size());
      ValidationFailure.Cause cause = validationFailure.getCauses().get(0);
      assertEquals(ToUTF8Action.ToUTF8Config.CHARSET, cause.getAttribute(CauseAttributes.STAGE_CONFIG));
      assertEquals(MOCK_NAME, cause.getAttribute(STAGE));
    }
  }
}
