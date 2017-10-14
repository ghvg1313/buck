/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android;

import com.facebook.buck.android.apkmodule.APKModule;
import com.facebook.buck.android.exopackage.ExopackageMode;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.util.MoreCollectors;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.EnumSet;
import java.util.Optional;

public class AndroidBinaryFilesInfo {
  private final AndroidGraphEnhancementResult enhancementResult;
  private final EnumSet<ExopackageMode> exopackageModes;
  private final boolean packageAssetLibraries;

  public AndroidBinaryFilesInfo(
      AndroidGraphEnhancementResult enhancementResult,
      EnumSet<ExopackageMode> exopackageModes,
      boolean packageAssetLibraries) {
    this.enhancementResult = enhancementResult;
    this.exopackageModes = exopackageModes;
    this.packageAssetLibraries = packageAssetLibraries;
  }

  DexFilesInfo getDexFilesInfo() {
    DexFilesInfo dexFilesInfo = enhancementResult.getDexFilesInfo();
    if (ExopackageMode.enabledForSecondaryDexes(exopackageModes)) {
      dexFilesInfo =
          new DexFilesInfo(
              dexFilesInfo.primaryDexPath,
              ImmutableSortedSet.of(),
              dexFilesInfo.proguardTextFilesPath);
    }
    return dexFilesInfo;
  }

  NativeFilesInfo getNativeFilesInfo() {
    Optional<ImmutableMap<APKModule, CopyNativeLibraries>> copyNativeLibraries =
        enhancementResult.getCopyNativeLibraries();

    boolean exopackageForNativeEnabled = ExopackageMode.enabledForNativeLibraries(exopackageModes);
    Optional<ImmutableSortedMap<APKModule, SourcePath>> nativeLibsDirs;
    if (exopackageForNativeEnabled) {
      nativeLibsDirs = Optional.empty();
    } else {
      nativeLibsDirs =
          copyNativeLibraries.map(
              cnl ->
                  cnl.entrySet()
                      .stream()
                      .collect(
                          MoreCollectors.toImmutableSortedMap(
                              e -> e.getKey(), e -> e.getValue().getSourcePathToNativeLibsDir())));
    }

    Optional<ImmutableSortedMap<APKModule, SourcePath>> nativeLibsAssetsDirs =
        copyNativeLibraries.map(
            cnl ->
                cnl.entrySet()
                    .stream()
                    .filter(
                        entry ->
                            !exopackageForNativeEnabled
                                || packageAssetLibraries
                                || !entry.getKey().isRootModule())
                    .collect(
                        MoreCollectors.toImmutableSortedMap(
                            e -> e.getKey(),
                            e -> e.getValue().getSourcePathToNativeLibsAssetsDir())));
    return new NativeFilesInfo(nativeLibsDirs, nativeLibsAssetsDirs);
  }

  ResourceFilesInfo getResourceFilesInfo() {
    return new ResourceFilesInfo(
        ImmutableSortedSet.copyOf(
            enhancementResult.getPackageableCollection().getPathsToThirdPartyJars()),
        enhancementResult.getPrimaryResourcesApkPath(),
        enhancementResult.getPrimaryApkAssetZips());
  }
}
