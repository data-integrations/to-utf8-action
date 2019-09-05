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

import com.google.common.base.Strings;
import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Macro;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.cdap.api.common.Bytes;
import io.cdap.cdap.api.plugin.PluginConfig;
import io.cdap.cdap.etl.api.FailureCollector;
import io.cdap.cdap.etl.api.PipelineConfigurer;
import io.cdap.cdap.etl.api.StageConfigurer;
import io.cdap.cdap.etl.api.action.Action;
import io.cdap.cdap.etl.api.action.ActionContext;
import io.cdap.cdap.etl.api.validation.ValidationException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Pattern;
import javax.annotation.Nullable;


/**
 * ToUTF8 Action Plugin - Converts the specified file to UTF-8.
 */
@Plugin(type = Action.PLUGIN_TYPE)
@Name(ToUTF8Action.PLUGIN_NAME)
@Description("Converts the specified files from a specified charset to UTF-8.")
public class ToUTF8Action extends Action {
  private static final Logger LOG = LoggerFactory.getLogger(ToUTF8Action.class);
  private static final int BUFFER_SIZE = 4096;

  public static final String PLUGIN_NAME = "ToUTF8";

  private final ToUTF8Config config;

  public ToUTF8Action(ToUTF8Config config) {
    this.config = config;
  }

  @Override
  public void configurePipeline(PipelineConfigurer pipelineConfigurer) {
    super.configurePipeline(pipelineConfigurer);
    StageConfigurer stageConfigurer = pipelineConfigurer.getStageConfigurer();
    config.validate(stageConfigurer.getFailureCollector());
  }

  @Override
  public void run(ActionContext context) throws Exception {
    config.validate(context.getFailureCollector());
    Path source = new Path(config.sourceFilePath);
    Path dest = new Path(config.destFilePath);

    FileSystem fileSystem = source.getFileSystem(new Configuration());
    fileSystem.mkdirs(dest.getParent());

    // Convert a single file
    if (fileSystem.exists(source) && fileSystem.getFileStatus(source).isFile()) {
      convertSingleFile(source, dest, fileSystem);
    } else {
      // Convert all the files in a directory
      PathFilter filter = new PathFilter() {
        private final Pattern pattern = Pattern.compile(config.getFileRegex());

        @Override
        public boolean accept(Path path) {
          return pattern.matcher(path.getName()).matches();
        }
      };
      FileStatus[] listFiles = fileSystem.globStatus(source, filter);
      if (listFiles == null || listFiles.length == 0 || (listFiles.length == 1 && listFiles[0].isDirectory())) {
        // try again without globbing action
        listFiles = fileSystem.listStatus(source, filter);
      }

      if (listFiles.length == 0) {
        LOG.warn("Not converting any files from source {} matching regular expression",
                 source.toString(), config.getFileRegex());
      }

      if (fileSystem.exists(dest) && fileSystem.isFile(dest)) {
        throw new IllegalArgumentException(
          String.format("Destination %s needs to be a directory since the source is a " +
                          "directory", config.destFilePath));
      }
      // create destination directory if necessary
      fileSystem.mkdirs(dest);

      for (FileStatus file : listFiles) {
        if (!file.isDirectory()) { // ignore directories
          source = file.getPath();
          convertSingleFile(source, dest, fileSystem);
        }
      }
    }
  }

  private void convertSingleFile(Path source, Path dest, FileSystem fileSystem) throws IOException {
    Path actualDestPath = (fileSystem.isDirectory(dest))
      ? new Path(dest.toString() + "/" + source.getName() + ".utf8")
      : dest;
    try (InputStream in = fileSystem.open(source);
         BufferedOutputStream out = new BufferedOutputStream(fileSystem.create(actualDestPath), BUFFER_SIZE)) {
      byte[] tempBytes = new byte[BUFFER_SIZE];
      int available = -1;
      while ((available = in.read(tempBytes)) > 0) {
        String tempString = Bytes.toString(ByteBuffer.wrap(tempBytes), Charset.forName(config.charset));
        out.write(Bytes.toBytes(tempString), 0, available);
      }
      out.flush();
      out.close();
    } catch (IOException e) {
      if (!config.getContinueOnError()) {
        throw new IOException(String.format("Failed to convert file %s to %s", source.toString(), dest.toString()));
      }
      LOG.warn(String.format("Exception convert file %s to %s", source.toString(), dest.toString()), e);
    }
  }

  /**
   * Config class that contains all properties required for running the unload command.
   */
  public static class ToUTF8Config extends PluginConfig {

    public static final String SOURCE_FILE_PATH = "sourceFilePath";
    public static final String DEST_FILE_PATH = "destFilePath";
    public static final String FILE_REGEX = "fileRegex";
    public static final String CHARSET = "charset";

    @Name(SOURCE_FILE_PATH)
    @Macro
    @Description("The source location where the file or files live. You can use glob syntax here such as *.dat.")
    private String sourceFilePath;

    @Name(DEST_FILE_PATH)
    @Macro
    @Description("The destination location where the converted files should be.")
    private String destFilePath;

    @Name(CHARSET)
    @Macro
    @Description("The source charset.")
    private String charset;

    @Name(FILE_REGEX)
    @Macro
    @Nullable
    @Description("A regular expression for filtering files such as .*\\.txt")
    private String fileRegex;

    @Macro
    @Nullable
    @Description("Set to true if this plugin should ignore errors.")
    private Boolean continueOnError;

    public String getFileRegex() {
      return (Strings.isNullOrEmpty(fileRegex)) ? ".*" : fileRegex;
    }

    public boolean getContinueOnError() {
      return continueOnError == null ? false : continueOnError;
    }

    public ToUTF8Config(String sourceFilePath, String destFilePath, @Nullable String fileRegex,
                        String charset, @Nullable Boolean continueOnError) {
      this.sourceFilePath = sourceFilePath;
      this.charset = charset;
      this.destFilePath = destFilePath;
      this.continueOnError = (continueOnError == null) ? false : continueOnError;
      this.fileRegex = (Strings.isNullOrEmpty(fileRegex)) ? ".*" : fileRegex;
    }

    /**
     * Validates the config parameters required for unloading the data.
     */
    private void validate(FailureCollector collector) throws ValidationException {
      if (Strings.isNullOrEmpty(sourceFilePath)) {
        collector.addFailure("Source file or folder is required.", "Set source file or folder.")
          .withConfigProperty(SOURCE_FILE_PATH);
        collector.getOrThrowException();
      }
      try {
        Path source = new Path(sourceFilePath);
        source.getFileSystem(new Configuration());
      } catch (IOException e) {
        collector.addFailure("Cannot determine the file system of the source file.", null)
          .withConfigProperty(SOURCE_FILE_PATH).withStacktrace(e.getStackTrace());
      }
      if (Strings.isNullOrEmpty(destFilePath)) {
        collector.addFailure("Destination file or folder is required.",
                             "Set destination file or folder.").withConfigProperty(DEST_FILE_PATH);
      }
      try {
        Pattern.compile(getFileRegex());
      } catch (Exception e) {
        collector.addFailure("The regular expression pattern provided is not a valid " +
                               "regular expression.", "Set correct regular expression pattern.")
          .withConfigProperty(FILE_REGEX);
      }
      try {
        Charset.forName(charset);
      } catch (UnsupportedCharsetException e) {
        collector.addFailure(e.getMessage(), "The charset entered is not valid. Please use a value " +
          "from https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html.")
          .withConfigProperty(CHARSET);
      }
      collector.getOrThrowException();
    }
  }
}
