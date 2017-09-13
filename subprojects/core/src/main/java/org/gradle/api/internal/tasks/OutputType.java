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

package org.gradle.api.internal.tasks;

import java.io.File;
import java.util.Collection;

import static org.gradle.api.internal.tasks.TaskPropertyUtils.toFile;

public enum OutputType implements TaskPropertyValidator {
    FILE() {
        @Override
        public void validate(TaskPropertySpec property, Object value, Collection<String> messages) {
            File file = toFile(value);
            assert file != null;
            if (file.exists()) {
                if (file.isDirectory()) {
                    messages.add(String.format("Cannot write to file '%s' specified for property '%s' as it is a directory.", file, property.getPropertyName()));
                }
                // else, assume we can write to anything that exists and is not a directory
            } else {
                for (File candidate = file.getParentFile(); candidate != null && !candidate.isDirectory(); candidate = candidate.getParentFile()) {
                    if (candidate.exists() && !candidate.isDirectory()) {
                        messages.add(String.format("Cannot write to file '%s' specified for property '%s', as ancestor '%s' is not a directory.", file, property.getPropertyName(), candidate));
                        break;
                    }
                }
            }
        }
    },

    DIRECTORY() {
        @Override
        public void validate(TaskPropertySpec property, Object value, Collection<String> messages) {
            File directory = toFile(value);
            assert directory != null;
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    messages.add(String.format("Directory '%s' specified for property '%s' is not a directory.", directory, property.getPropertyName()));
                }
            } else {
                for (File candidate = directory.getParentFile(); candidate != null && !candidate.isDirectory(); candidate = candidate.getParentFile()) {
                    if (candidate.exists() && !candidate.isDirectory()) {
                        messages.add(String.format("Cannot write to directory '%s' specified for property '%s', as ancestor '%s' is not a directory.", directory, property.getPropertyName(), candidate));
                        return;
                    }
                }
            }
        }
    }
}
