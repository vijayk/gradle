/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.tasks;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshotter;
import org.gradle.api.internal.changedetection.state.GenericFileCollectionSnapshotter;
import org.gradle.api.internal.changedetection.state.InputPathNormalizationStrategy;
import org.gradle.api.internal.changedetection.state.PathNormalizationStrategy;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.PathSensitivity;

import java.util.Collection;

import static org.gradle.api.internal.changedetection.state.InputPathNormalizationStrategy.ABSOLUTE;

@NonNullApi
public class DefaultTaskInputFilePropertySpec extends AbstractTaskInputsDeprecatingTaskPropertyBuilder implements DeclaredTaskInputFileProperty {

    private final TaskPropertyFileCollection files;
    private final TaskPropertyValidator validator;
    private boolean skipWhenEmpty;
    private boolean optional;
    private PathNormalizationStrategy pathNormalizationStrategy = ABSOLUTE;
    private Class<? extends FileCollectionSnapshotter> snapshotter = GenericFileCollectionSnapshotter.class;

    public DefaultTaskInputFilePropertySpec(String taskName, FileResolver resolver, TaskPropertyValidator validator, Object paths) {
        this(taskName, resolver, validator, paths, paths);
    }

    public DefaultTaskInputFilePropertySpec(String taskName, FileResolver resolver, TaskPropertyValidator validator, Object declaredPaths, Object paths) {
        this.validator = validator;
        this.files = new TaskPropertyFileCollection(taskName, "input", this, resolver, declaredPaths, paths);
    }

    @Override
    public FileCollection getPropertyFiles() {
        return files;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal withPropertyName(String propertyName) {
        setPropertyName(propertyName);
        return this;
    }

    public boolean isSkipWhenEmpty() {
        return skipWhenEmpty;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal skipWhenEmpty(boolean skipWhenEmpty) {
        this.skipWhenEmpty = skipWhenEmpty;
        return this;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal skipWhenEmpty() {
        return skipWhenEmpty(true);
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal optional() {
        return optional(true);
    }

    @Override
    public PathNormalizationStrategy getPathNormalizationStrategy() {
        return pathNormalizationStrategy;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal withPathSensitivity(PathSensitivity sensitivity) {
        return withPathNormalizationStrategy(InputPathNormalizationStrategy.valueOf(sensitivity));
    }

    @Override
    public TaskInputFilePropertyBuilderInternal withPathNormalizationStrategy(PathNormalizationStrategy pathNormalizationStrategy) {
        this.pathNormalizationStrategy = pathNormalizationStrategy;
        return this;
    }

    @Override
    public TaskInputFilePropertyBuilderInternal withSnapshotter(Class<? extends FileCollectionSnapshotter> snapshotter) {
        this.snapshotter = snapshotter;
        return this;
    }

    @Override
    public Class<? extends FileCollectionSnapshotter> getSnapshotter() {
        return snapshotter;
    }

    @Override
    public void validate(Collection<String> messages) {
        files.validate(optional, validator, messages);
    }

    @Override
    public String toString() {
        return getPropertyName() + " (" + pathNormalizationStrategy + ")";
    }

    @Override
    public int compareTo(TaskPropertySpec o) {
        return getPropertyName().compareTo(o.getPropertyName());
    }
}
