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
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.SourceComponentIdentifier;
import org.gradle.api.artifacts.component.SourceComponentSelector;
import org.gradle.vcs.VersionControlSpec;

public class DefaultSourceComponentSelector implements SourceComponentSelector {
    private final VersionControlSpec versionControlSpec;
    private final String projectPath;
    private final String buildName;
    private final String displayName;

    private DefaultSourceComponentSelector(VersionControlSpec versionControlSpec, String projectPath, String buildName) {
        this.versionControlSpec = versionControlSpec;
        this.projectPath = projectPath;
        this.buildName = buildName;
        this.displayName = versionControlSpec.getDisplayName() + " " + createDisplayName(buildName, projectPath);
    }

    private static String createDisplayName(String buildName, String projectPath) {
        if (":".equals(buildName)) {
            return "project " + projectPath;
        }
        return "project :" + buildName + projectPath;
    }

    public static SourceComponentSelector newSelector(VersionControlSpec versionControlSpec, String projectPath, String buildName) {
        return new DefaultSourceComponentSelector(versionControlSpec, projectPath, buildName);
    }

    @Override
    public VersionControlSpec getRepository() {
        return versionControlSpec;
    }

    @Override
    public String getBuildName() {
        return buildName;
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
    public String toString() {
        return getDisplayName();
    }

    @Override
    public boolean matchesStrictly(ComponentIdentifier identifier) {
        if (identifier instanceof SourceComponentIdentifier) {
            SourceComponentIdentifier sourceComponentIdentifier = (SourceComponentIdentifier) identifier;
            return Objects.equal(buildName, sourceComponentIdentifier.getBuild().getName())
                && Objects.equal(projectPath, sourceComponentIdentifier.getProjectPath())
                && Objects.equal(versionControlSpec, sourceComponentIdentifier.getRepository());
        }
        return false;
    }

    // TODO: equals and hashCode
}
