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
import org.gradle.api.NonNullApi;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Collection;

@NonNullApi
public class TaskPropertyInfo implements Comparable<TaskPropertyInfo> {
    private static final TaskPropertyValue NO_OP_VALUE = new TaskPropertyValue() {
        public Object getValue() {
            return null;
        }

        public void checkNotNull(Collection<String> messages) {
        }

        public void checkValid(Collection<String> messages) {
        }
    };

    private final TaskPropertyInfo parent;
    private final String name;
    private final InputOutputPropertyInfo inputOutputPropertyInfo;

    public static String fullName(@Nullable TaskPropertyInfo parent, String relativeName) {
        return parent == null ? relativeName : parent.getName() + '.' + relativeName;
    }

    TaskPropertyInfo(@Nullable TaskPropertyInfo parent, InputOutputPropertyInfo inputOutputPropertyInfo) {
        this.parent = parent;
        this.inputOutputPropertyInfo = inputOutputPropertyInfo;
        this.name = fullName(parent, inputOutputPropertyInfo.getName());
    }

    @Override
    public String toString() {
        return String.format("@%s %s", inputOutputPropertyInfo.getPropertyType().getSimpleName(), name);
    }

    public String getName() {
        return name;
    }

    public Class<? extends Annotation> getPropertyType() {
        return inputOutputPropertyInfo.getPropertyType();
    }

    public UpdateAction getConfigureAction() {
        return inputOutputPropertyInfo.getConfigureAction();
    }

    public TaskPropertyValue getValue(Object rootObject) {
        final Object bean;
        if (parent != null) {
            TaskPropertyValue parentValue = parent.getValue(rootObject);
            if (parentValue.getValue() == null) {
                return NO_OP_VALUE;
            }
            bean = parentValue.getValue();
        } else {
            bean = rootObject;
        }

        final InputOutputPropertyValue value = inputOutputPropertyInfo.getValue(bean);

        return new TaskPropertyValue() {
            @Override
            public Object getValue() {
                return value.getValue();
            }

            @Override
            public void checkNotNull(Collection<String> messages) {
                value.checkNotNull(name, messages);
            }

            @Override
            public void checkValid(Collection<String> messages) {
                value.checkValid(name, messages);
            }
        };
    }

    @Override
    public int compareTo(TaskPropertyInfo o) {
        return name.compareTo(o.getName());
    }

    public void addValidationMessages(ImmutableCollection.Builder<TaskClassValidationMessage> validationMessages) {
        for (String message : inputOutputPropertyInfo.getValidationMessages()) {
            validationMessages.add(TaskClassValidationMessage.property(name, message));
        }
    }
}
