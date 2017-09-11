/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.execution.taskgraph;


import org.gradle.api.Task;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.project.taskfactory.TaskClassValidator;
import org.gradle.api.internal.project.taskfactory.TaskClassValidatorExtractor;
import org.gradle.api.specs.Specs;
import org.gradle.composite.internal.IncludedBuildTaskResource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TaskInfoFactory {
    private final Map<Task, TaskInfo> nodes = new HashMap<Task, TaskInfo>();
    private final TaskClassValidatorExtractor taskClassValidatorExtractor;

    public TaskInfoFactory(TaskClassValidatorExtractor taskClassValidatorExtractor) {
        this.taskClassValidatorExtractor = taskClassValidatorExtractor;
    }

    public Set<Task> getTasks() {
        return nodes.keySet();
    }

    public TaskInfo createNode(Task task) {
        TaskInfo node = nodes.get(task);
        if (node == null) {
            addInputsAndOutputs(task);
            if (task instanceof IncludedBuildTaskResource) {
                node = new TaskResourceTaskInfo((TaskInternal) task);
            } else {
                node = new TaskInfo((TaskInternal) task);
            }
            nodes.put(task, node);
        }
        return node;
    }

    private void addInputsAndOutputs(Task task) {
        TaskClassValidator validator = taskClassValidatorExtractor.extractValidator(task);
        if (validator.hasAnythingToValidate()) {
            validator.addInputsAndOutputs((TaskInternal) task);
        }
        // Enabled caching if task type is annotated with @CacheableTask
        if (validator.isCacheable()) {
            task.getOutputs().cacheIf("Annotated with @CacheableTask", Specs.SATISFIES_ALL);
        }
    }

    public void clear() {
        nodes.clear();
    }

    private static class TaskResourceTaskInfo extends TaskInfo {
        public TaskResourceTaskInfo(TaskInternal task) {
            super(task);
            doNotRequire();
        }

        @Override
        public void require() {
            // Ignore
        }

        @Override
        public boolean isComplete() {
            IncludedBuildTaskResource task = (IncludedBuildTaskResource) getTask();
            return task.isComplete();
        }
    }
}
