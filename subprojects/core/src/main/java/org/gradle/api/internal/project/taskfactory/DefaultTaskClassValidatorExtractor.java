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

package org.gradle.api.internal.project.taskfactory;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import org.gradle.api.NonNullApi;
import org.gradle.api.Task;
import org.gradle.api.tasks.CacheableTask;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;

@NonNullApi
public class DefaultTaskClassValidatorExtractor implements TaskClassValidatorExtractor {

    private final InputOutputPropertyExtractor inputOutputPropertyExtractor;

    public DefaultTaskClassValidatorExtractor(InputOutputPropertyExtractor inputOutputPropertyExtractor) {
        this.inputOutputPropertyExtractor = inputOutputPropertyExtractor;
    }

    @Override
    public TaskClassValidator extractValidator(Task task) {
        return extractValidator(task.getClass(), task);
    }

    @Override
    public TaskClassValidator extractValidator(Class<? extends Task> type) {
        return extractValidator(type, null);
    }

    public TaskClassValidator extractValidator(Class<? extends Task> type, @Nullable Task task) {
        boolean cacheable = type.isAnnotationPresent(CacheableTask.class);
        ImmutableSortedSet.Builder<TaskPropertyInfo> annotatedPropertiesBuilder = ImmutableSortedSet.naturalOrder();
        ImmutableList.Builder<TaskClassValidationMessage> validationMessages = ImmutableList.builder();
        Queue<TypeEntry> queue = new ArrayDeque<TypeEntry>();
        queue.add(new TypeEntry(null, type, task));
        while (!queue.isEmpty()) {
            parseProperties(queue.remove(), annotatedPropertiesBuilder, validationMessages, cacheable, queue);
        }
        return new TaskClassValidator(annotatedPropertiesBuilder.build(), validationMessages.build(), cacheable);
    }

    private void parseProperties(final TypeEntry entry, ImmutableSet.Builder<TaskPropertyInfo> annotatedProperties, final ImmutableCollection.Builder<TaskClassValidationMessage> validationMessages, final boolean cacheable, Queue<TypeEntry> queue) {
        InputsOutputsInfo inputOutputPropertyInfos = inputOutputPropertyExtractor.extractProperties(entry.getType(), cacheable);
        for (InputOutputPropertyInfo inputOutputPropertyInfo : inputOutputPropertyInfos.getInputsAndOutputs()) {
            TaskPropertyInfo property = new TaskPropertyInfo(entry.getParent(), inputOutputPropertyInfo);
            annotatedProperties.add(property);
            if (inputOutputPropertyInfo.isNested()) {
                Object value = entry.getInstance() == null ? null : inputOutputPropertyInfo.getValue(entry.getInstance()).getValue();
                queue.add(new TypeEntry(property, inputOutputPropertyInfo.getNestedType(), value));
            }
            property.addValidationMessages(validationMessages);
        }
        for (String nonAnnotatedProperty : inputOutputPropertyInfos.getNonAnnotatedProperties()) {
            validationMessages.add(TaskClassValidationMessage.property(TaskPropertyInfo.fullName(entry.getParent(), nonAnnotatedProperty), "is not annotated with an input or output annotation"));
        }
    }

    private static class TypeEntry {
        private final TaskPropertyInfo parent;
        private final Class<?> type;
        private final Object instance;

        public TypeEntry(@Nullable TaskPropertyInfo parent, Class<?> type, @Nullable Object instance) {
            this.parent = parent;
            this.type = type;
            this.instance = instance;
        }

        @Nullable
        public TaskPropertyInfo getParent() {
            return parent;
        }

        public Class<?> getType() {
            return instance != null ? instance.getClass() : type;
        }

        @Nullable
        public Object getInstance() {
            return instance;
        }

    }
}
