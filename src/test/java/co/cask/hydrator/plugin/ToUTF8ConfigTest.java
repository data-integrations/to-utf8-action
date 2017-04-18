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

package co.cask.hydrator.plugin;

import co.cask.cdap.etl.mock.common.MockPipelineConfigurer;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.junit.Assert;
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
  private static final String ISO_8859_FILE_NAME = "/20150320_clo_prod_cln.dat";
  private static final String UTF_8_FILE_NAME = "/20150320_clo_prod_cln.dat.utf8";

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
    URL iso88591File = this.getClass().getResource(ISO_8859_FILE_NAME);
    File destFolder = temporaryFolder.newFolder();
    System.out.println("Source Path: " + iso88591File.getFile());
    System.out.println("Dest Path: " + destFolder.getPath() + UTF_8_FILE_NAME);
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config(iso88591File.getFile(),
                                                                     destFolder.getPath() + UTF_8_FILE_NAME,
                                                                     null, "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    new ToUTF8Action(config).run(null);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, "/" + destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testFolder() throws Exception {
    URL iso88591File = this.getClass().getResource(ISO_8859_FILE_NAME);

    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config(new File(iso88591File.getFile()).getParent(),
                                                                     destFolder.getPath(),
                                                                     null, "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    new ToUTF8Action(config).run(null);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, "/" + destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testFolderWithGlob() throws Exception {
    URL iso88591File = this.getClass().getResource(ISO_8859_FILE_NAME);

    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config =
      new ToUTF8Action.ToUTF8Config(new File(iso88591File.getFile()).getParent() + "/*.dat",
                                    destFolder.getPath(),
                                    null, "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    new ToUTF8Action(config).run(null);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, "/" + destFolder.listFiles(filter)[0].getName());
  }

  @Test
  public void testFolderWithRegEx() throws Exception {
    URL iso88591File = this.getClass().getResource(ISO_8859_FILE_NAME);

    File destFolder = temporaryFolder.newFolder();
    ToUTF8Action.ToUTF8Config config = new ToUTF8Action.ToUTF8Config(new File(iso88591File.getFile()).getParent(),
                                                                     destFolder.getPath(),
                                                                     ".*\\.dat", "ISO-8859-1", false);
    MockPipelineConfigurer configurer = new MockPipelineConfigurer(null);
    new ToUTF8Action(config).configurePipeline(configurer);
    new ToUTF8Action(config).run(null);
    assertEquals(1, destFolder.listFiles(filter).length);
    assertEquals(UTF_8_FILE_NAME, "/" + destFolder.listFiles(filter)[0].getName());
  }
}
