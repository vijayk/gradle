/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.state;

import com.google.common.hash.HashCode;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.caching.internal.DefaultBuildCacheHasher;

/**
 * Builds a {@link FileCollectionSnapshot} for a compile classpath.
 *
 * We only take class files in jar files and class files in directories into account.
 */
public class CompileClasspathSnapshotBuilder extends AbstractClasspathSnapshotBuilder {
    private final ResourceSnapshotterCacheService cacheService;
    private final JarHasher runtimeJarHasher;
    private final byte[] runtimeJarHasherConfigurationHash;

    public CompileClasspathSnapshotBuilder(ResourceHasher classpathResourceHasher, ResourceHasher runtimeClasspathHasher, ResourceSnapshotterCacheService cacheService, StringInterner stringInterner) {
        super(classpathResourceHasher, cacheService, stringInterner);
        this.cacheService = cacheService;
        this.runtimeJarHasher = new JarHasher(runtimeClasspathHasher, stringInterner);
        DefaultBuildCacheHasher defaultBuildCacheHasher = new DefaultBuildCacheHasher();
        runtimeClasspathHasher.appendConfigurationToHasher(defaultBuildCacheHasher);
        this.runtimeJarHasherConfigurationHash = defaultBuildCacheHasher.hash().asBytes();
    }

    @Override
    protected void visitNonJar(RegularFileSnapshot file) {
    }

    @Override
    protected RegularFileSnapshot normalizeJarHash(RegularFileSnapshot jarFile) {
        HashCode hashCode = cacheService.hashFile(jarFile, runtimeJarHasher, runtimeJarHasherConfigurationHash);
        return hashCode == null ? null : jarFile.withContentHash(hashCode);
    }
}
