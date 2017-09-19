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

package org.gradle.internal.buildoption;

import org.apache.commons.lang.StringUtils;
import org.gradle.cli.CommandLineArgumentException;
import org.gradle.cli.CommandLineParser;
import org.gradle.cli.ParsedCommandLine;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Represents a option for a build provided by the user via Gradle property and/or a command line option.
 *
 * @since 4.3
 */
public interface BuildOption<T> {

    @Nullable
    String getGradleProperty();

    void applyFromProperty(Map<String, String> properties, T settings);

    void configure(CommandLineParser parser);

    void applyFromCommandLine(ParsedCommandLine options, T settings);

    enum Origin {
        GRADLE_PROPERTY {
            @Override
            public void handleInvalidValue(BuildOption<?> option, String value, String hint) {
                String message = String.format("Value '%s' given for %s Gradle property is invalid%s.", value, option.getGradleProperty(), hintMessage(hint));
                throw new IllegalArgumentException(message);
            }
        },
        COMMAND_LINE {
            @Override
            public void handleInvalidValue(BuildOption<?> option, String value, String hint) {
                if (!(option instanceof StringBuildOption)) {
                    throw new IllegalStateException("Can't get command line from non StringBuildOption");
                }
                String commandLineOption = StringBuildOption.class.cast(option).getCommandLineOption();
                String message = String.format("Argument value '%s' given for --%s option is invalid%s.", value, commandLineOption, hintMessage(hint));
                throw new CommandLineArgumentException(message);
            }
        };

        public abstract void handleInvalidValue(BuildOption<?> option, String value, String hint);

        public void handleInvalidValue(BuildOption<?> option, String value) {
            handleInvalidValue(option, value, null);
        }

        protected String hintMessage(String hint) {
            if (StringUtils.isBlank(hint)) {
                return "";
            } else {
                return String.format(" (%s)", hint);
            }
        }
    }
}
