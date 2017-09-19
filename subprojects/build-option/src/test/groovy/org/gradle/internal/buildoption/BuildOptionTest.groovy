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

package org.gradle.internal.buildoption

import org.gradle.cli.CommandLineArgumentException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class BuildOptionTest extends Specification {
    private static final String VALUE = '0'
    private static final String HINT = 'must be positive'

    @Shared
    StringBuildOption stringBuildOption = Mock(StringBuildOption)
    @Shared
    BuildOption nonStringBuildOption = Mock(BuildOption)

    def setupSpec() {
        stringBuildOption.getGradleProperty() >> 'org.gradle.property'
        stringBuildOption.getCommandLineOption() >> 'option'
        nonStringBuildOption.getGradleProperty() >> 'org.gradle.property'
    }

    @Unroll
    def "can handle invalid value for Gradle property with empty #hint"() {
        when:
        BuildOption.Origin.GRADLE_PROPERTY.handleInvalidValue(stringBuildOption, VALUE, hint)

        then:
        Throwable t = thrown(IllegalArgumentException)
        t.message == "Value '0' given for org.gradle.property Gradle property is invalid."

        where:
        hint << ['', ' ', null]
    }

    @Unroll
    def "can handle invalid value for Gradle property with concrete hint"() {
        when:
        BuildOption.Origin.GRADLE_PROPERTY.handleInvalidValue(buildOption, VALUE, HINT)

        then:
        Throwable t = thrown(IllegalArgumentException)
        t.message == "Value '0' given for org.gradle.property Gradle property is invalid (must be positive)."

        where:
        buildOption << [stringBuildOption, nonStringBuildOption]
    }

    def "can handle invalid value for command line option with concrete hint"() {
        when:
        BuildOption.Origin.COMMAND_LINE.handleInvalidValue(stringBuildOption, VALUE, HINT)

        then:
        Throwable t = thrown(CommandLineArgumentException)
        t.message == "Argument value '0' given for --option option is invalid (must be positive)."
    }

    def "throws exception when handling non StringBuildOption's command line option"() {
        when:
        BuildOption.Origin.COMMAND_LINE.handleInvalidValue(nonStringBuildOption, VALUE, HINT)

        then:
        thrown(IllegalStateException)
    }
}
