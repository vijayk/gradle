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

package org.gradle.api.internal.artifacts.vcs;

import com.google.common.base.Objects;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.artifacts.component.SourceComponentIdentifier;
import org.gradle.vcs.VersionControlSpec;

public class DefaultSourceComponentIdentifier implements SourceComponentIdentifier {
    private final VersionControlSpec versionControlSpec;
    private final String projectPath;
    private final BuildIdentifier buildIdentifier;
    private final String displayName;

    private DefaultSourceComponentIdentifier(VersionControlSpec versionControlSpec, String projectPath, BuildIdentifier buildIdentifier) {
        this.versionControlSpec = versionControlSpec;
        this.projectPath = projectPath;
        this.buildIdentifier = buildIdentifier;
        this.displayName = versionControlSpec.getDisplayName() + " " + fullPath(buildIdentifier, projectPath);
    }

    @Override
    public VersionControlSpec getRepository() {
        return versionControlSpec;
    }

    @Override
    public BuildIdentifier getBuild() {
        return buildIdentifier;
    }

    @Override
    public String getProjectPath() {
        return projectPath;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultSourceComponentIdentifier that = (DefaultSourceComponentIdentifier) o;
        return Objects.equal(projectPath, that.projectPath)
            && Objects.equal(buildIdentifier, that.buildIdentifier)
            && Objects.equal(versionControlSpec, that.versionControlSpec);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectPath, buildIdentifier, versionControlSpec);
    }

    @Override
    public String toString() {
        return displayName;
    }

    private static String fullPath(BuildIdentifier build, String projectPath) {
        if (build.getName().equals(":")) {
            return projectPath;
        } else if (projectPath.equals(":")) {
            return ":" + build.getName();
        } else {
            return ":" + build.getName() + projectPath;
        }
    }

    public static SourceComponentIdentifier newIdentifier(VersionControlSpec versionControlSpec, String projectPath, BuildIdentifier buildIdentifier) {
        return new DefaultSourceComponentIdentifier(versionControlSpec, projectPath, buildIdentifier);
    }
}
