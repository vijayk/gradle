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

package org.gradle.api.internal.project.taskfactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.gradle.api.NonNullApi;
import org.gradle.api.internal.TaskInternal;
import org.gradle.internal.Factory;
import org.gradle.internal.reflect.JavaReflectionUtil;
import org.gradle.util.DeprecationLogger;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;

@NonNullApi
public class InputOutputPropertyInfo implements Comparable<InputOutputPropertyInfo> {
    private static final ValidationAction NO_OP_VALIDATION_ACTION = new ValidationAction() {
        public void validate(String propertyName, Object value, Collection<String> messages) {
        }
    };
    private static final UpdateAction NO_OP_CONFIGURATION_ACTION = new UpdateAction() {
        public void update(TaskInternal task, String propertyName, Callable<Object> futureValue) {
        }
    };

    private final String propertyName;
    private final Class<? extends Annotation> propertyType;
    private final Method method;
    private final ValidationAction validationAction;
    private final ImmutableList<String> validationMessages;
    private final UpdateAction configureAction;
    private final boolean optional;
    private final Class<?> nestedType;

    InputOutputPropertyInfo(String propertyName, Class<? extends Annotation> propertyType, Method method, @Nullable ValidationAction validationAction, ImmutableList<String> validationMessages, @Nullable UpdateAction configureAction, boolean optional, @Nullable Class<?> nestedType) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.method = method;
        this.validationAction = validationAction == null ? NO_OP_VALIDATION_ACTION : validationAction;
        this.validationMessages = validationMessages;
        this.configureAction = configureAction == null ? NO_OP_CONFIGURATION_ACTION : configureAction;
        this.optional = optional;
        this.nestedType = nestedType;
    }

    @Override
    public String toString() {
        return String.format("@%s %s", propertyType.getSimpleName(), propertyName);
    }

    public String getName() {
        return propertyName;
    }

    public Class<? extends Annotation> getPropertyType() {
        return propertyType;
    }

    public UpdateAction getConfigureAction() {
        return configureAction;
    }

    public InputOutputPropertyValue getValue(final Object rootObject) {
        final Object value = DeprecationLogger.whileDisabled(new Factory<Object>() {
            public Object create() {
                return JavaReflectionUtil.method(Object.class, method).invoke(rootObject);
            }
        });

        return new InputOutputPropertyValue() {
            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public void checkNotNull(String propertyName, Collection<String> messages) {
                if (value == null && !optional) {
                    messages.add(String.format("No value has been specified for property '%s'.", propertyName));
                }
            }

            @Override
            public void checkValid(String propertyName, Collection<String> messages) {
                if (value != null) {
                    validationAction.validate(propertyName, value, messages);
                }
            }
        };
    }

    @Override
    public int compareTo(InputOutputPropertyInfo o) {
        return propertyName.compareTo(o.getName());
    }

    public boolean isNested() {
        return nestedType != null;
    }

    public Class<?> getNestedType() {
        Preconditions.checkNotNull(nestedType);
        return nestedType;
    }

    public ImmutableList<String> getValidationMessages() {
        return validationMessages;
    }
}
