// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.cpp;

import com.google.devtools.build.lib.testutil.TestConstants;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.DefaultCpuToolchain;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.ToolPath;
import com.google.protobuf.TextFormat;

import java.io.IOException;

/**
 * Helper class for the creation of crosstool toolchain factories.
 */
public class CrosstoolConfigurationHelper {
  public static Path overwriteCrosstoolFile(Path workspace, String content) throws IOException {
    Path crosstool = workspace.getRelative(TestConstants.MOCK_CROSSTOOL_PATH + "/CROSSTOOL");
    long newMTime = crosstool.exists() ? crosstool.getLastModifiedTime() + 1 : -1;
    crosstool.delete();
    FileSystemUtils.createDirectoryAndParents(crosstool.getParentDirectory());
    FileSystemUtils.writeContentAsLatin1(crosstool, content);
    crosstool.setLastModifiedTime(newMTime);
    return crosstool;
  }

  /**
   * Overwrites the default CROSSTOOL file with one obtained by merging the simple complete
   * toolchain with the given additional partial toolchain. Only {@code --cpu=piii} is valid.
   */
  public static void overwriteCrosstoolWithToolchain(
      Path workspace, CrosstoolConfig.CToolchain partial) throws IOException {
    CrosstoolConfig.CrosstoolRelease.Builder release =
        CrosstoolConfig.CrosstoolRelease.newBuilder();
    release.mergeFrom(simpleCompleteToolchainProto());
    CrosstoolConfig.CToolchain.Builder toolchain = CrosstoolConfig.CToolchain.newBuilder();
    toolchain.mergeFrom(release.getToolchain(0));
    toolchain.mergeFrom(partial);
    release.setToolchain(0, toolchain.build());
    overwriteCrosstoolFile(workspace, TextFormat.printToString(release.build()));
  }

  /**
   * Overwrites the default CROSSTOOL file with a reasonable toolchain.
   */
  public static void overwriteCrosstoolWithSimpleCompleteToolchain(Path workspace)
      throws IOException {
    overwriteCrosstoolFile(workspace, TextFormat.printToString(simpleCompleteToolchainProto()));
  }

  private static CrosstoolConfig.CrosstoolRelease simpleCompleteToolchainProto() {
    CrosstoolConfig.CrosstoolRelease.Builder builder =
        CrosstoolConfig.CrosstoolRelease.newBuilder()
            .setMajorVersion("12")
            .setMinorVersion("0")
            .setDefaultTargetCpu("k8")
            .addDefaultToolchain(
                DefaultCpuToolchain.newBuilder()
                    .setCpu("k8")
                    .setToolchainIdentifier("k8-toolchain"));
    CrosstoolConfig.CToolchain.Builder toolchainBuilder = newIncompleteToolchain();
    toolchainBuilder
        .setToolchainIdentifier("k8-toolchain")
        .setHostSystemName("i686-unknown-linux-gnu")
        .setTargetSystemName("i686-unknown-linux-gnu")
        .setTargetCpu("k8")
        .setTargetLibc("glibc-2.3.6-grte")
        .setCompiler("gcc-4.3.1")
        .setAbiVersion("gcc-3.4")
        .setAbiLibcVersion("2.3.2")
        .addCxxBuiltinIncludeDirectory("/include/directory");
    builder.addToolchain(toolchainBuilder);
    return builder.build();
  }

  private static CrosstoolConfig.CToolchain.Builder newIncompleteToolchain() {
    CrosstoolConfig.CToolchain.Builder builder = CrosstoolConfig.CToolchain.newBuilder();
    for (String tool :
        new String[] {
          "ar",
          "cpp",
          "gcc",
          "gcov",
          "ld",
          "compat-ld",
          "nm",
          "objcopy",
          "objdump",
          "strip",
          "dwp",
          "gcov-tool"
        }) {
      builder.addToolPath(ToolPath.newBuilder().setName(tool).setPath(tool));
    }
    return builder;
  }
}
