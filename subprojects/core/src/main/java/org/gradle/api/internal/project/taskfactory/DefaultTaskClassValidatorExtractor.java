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
import org.gradle.api.Task;
import org.gradle.api.tasks.CacheableTask;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;

public class DefaultTaskClassValidatorExtractor implements TaskClassValidatorExtractor {

    private final InputOutputPropertyExtractor inputOutputPropertyExtractor;

    public DefaultTaskClassValidatorExtractor(PropertyAnnotationHandler... customAnnotationHandlers) {
        this(Arrays.asList(customAnnotationHandlers));
    }

    public DefaultTaskClassValidatorExtractor(Iterable<? extends PropertyAnnotationHandler> customAnnotationHandlers) {
        inputOutputPropertyExtractor = new InputOutputPropertyExtractor(customAnnotationHandlers);
    }

    @Override
    public TaskClassValidator extractValidator(Class<? extends Task> type) {
        boolean cacheable = type.isAnnotationPresent(CacheableTask.class);
        ImmutableSortedSet.Builder<TaskPropertyInfo> annotatedPropertiesBuilder = ImmutableSortedSet.naturalOrder();
        ImmutableList.Builder<TaskClassValidationMessage> validationMessages = ImmutableList.builder();
        Queue<TypeEntry> queue = new ArrayDeque<TypeEntry>();
        queue.add(new TypeEntry(null, type));
        while (!queue.isEmpty()) {
            TypeEntry entry = queue.remove();
            parseProperties(entry.parent, entry.type, annotatedPropertiesBuilder, validationMessages, cacheable, queue);
        }
        return new TaskClassValidator(annotatedPropertiesBuilder.build(), validationMessages.build(), cacheable);
    }

    private <T> void parseProperties(final TaskPropertyInfo parent, Class<T> type, ImmutableSet.Builder<TaskPropertyInfo> annotatedProperties, final ImmutableCollection.Builder<TaskClassValidationMessage> validationMessages, final boolean cacheable, Queue<TypeEntry> queue) {
        Set<InputOutputPropertyInfo> inputOutputPropertyInfos = inputOutputPropertyExtractor.extractProperties(type, cacheable);
        for (InputOutputPropertyInfo inputOutputPropertyInfo : inputOutputPropertyInfos) {
            TaskPropertyInfo property = new TaskPropertyInfo(parent, inputOutputPropertyInfo);
            annotatedProperties.add(property);
            if (inputOutputPropertyInfo.isNested()) {
                queue.add(new TypeEntry(property, inputOutputPropertyInfo.getNestedType()));
            }
            property.addValidationMessages(validationMessages);
        }
    }

    private static class TypeEntry {
        private final TaskPropertyInfo parent;
        private final Class<?> type;

        public TypeEntry(TaskPropertyInfo parent, Class<?> type) {
            this.parent = parent;
            this.type = type;
        }
    }
}
