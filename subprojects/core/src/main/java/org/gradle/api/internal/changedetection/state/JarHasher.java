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
import org.gradle.api.UncheckedIOException;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.caching.internal.BuildCacheHasher;
import org.gradle.util.DeprecationLogger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.zip.ZipException;

public class JarHasher implements RegularFileHasher, ConfigurableNormalizer {

    private final ResourceHasher classpathResourceHasher;
    private final StringInterner stringInterner;

    public JarHasher(ResourceHasher classpathResourceHasher, StringInterner stringInterner) {
        this.classpathResourceHasher = classpathResourceHasher;
        this.stringInterner = stringInterner;
    }

    @Nullable
    @Override
    public HashCode hash(RegularFileSnapshot fileSnapshot) {
        try {
            ClasspathEntrySnapshotBuilder classpathEntrySnapshotBuilder = newClasspathEntrySnapshotBuilder();
            new ZipTree(fileSnapshot).visit(classpathEntrySnapshotBuilder);
            return classpathEntrySnapshotBuilder.getHash();
        } catch (ZipException e) {
            // ZipExceptions point to a problem with the Zip, we try to be lenient for now.
            return hashMalformedZip(fileSnapshot);
        } catch (IOException e) {
            // IOExceptions other than ZipException are failures.
            throw new UncheckedIOException("Error snapshotting jar [" + fileSnapshot.getName() + "]", e);
        } catch (Exception e) {
            // Other Exceptions can be thrown by invalid zips, too. See https://github.com/gradle/gradle/issues/1581.
            return hashMalformedZip(fileSnapshot);
        }
    }

    @Override
    public void appendConfigurationToHasher(BuildCacheHasher hasher) {
        hasher.putString(getClass().getName());
        classpathResourceHasher.appendConfigurationToHasher(hasher);
    }

    private HashCode hashMalformedZip(FileSnapshot fileSnapshot) {
        DeprecationLogger.nagUserWith("Malformed jar [" + fileSnapshot.getName() + "] found on classpath. Gradle 5.0 will no longer allow malformed jars on a classpath.");
        return fileSnapshot.getContent().getContentMd5();
    }

    private ClasspathEntrySnapshotBuilder newClasspathEntrySnapshotBuilder() {
        return new ClasspathEntrySnapshotBuilder(classpathResourceHasher, stringInterner);
    }
}
