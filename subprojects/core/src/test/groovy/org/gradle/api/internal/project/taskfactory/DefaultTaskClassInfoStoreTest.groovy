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

package org.gradle.api.internal.project.taskfactory

import com.google.common.util.concurrent.UncheckedExecutionException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import spock.lang.Specification

class DefaultTaskClassInfoStoreTest extends Specification {

    def taskClassInfoStore = new DefaultTaskClassInfoStore()

    private static class SimpleTask extends DefaultTask {
        @TaskAction
        void doStuff() {}
    }

    private static class MultipleActions extends DefaultTask {
        @TaskAction
        void doStuff() {}

        @TaskAction
        void doMoreStuff() {}
    }

    private static class MultipleIncrementalActions extends DefaultTask {
        @TaskAction
        void doStuff(IncrementalTaskInputs inputs) {}

        @TaskAction
        void doMoreStuff(IncrementalTaskInputs inputs) {}
    }

    private static class IncrementalTask extends DefaultTask {
        @TaskAction
        void doStuffIncrementally(IncrementalTaskInputs inputs) {}
    }

    def "class infos are cached"() {
        def info = taskClassInfoStore.getTaskClassInfo(SimpleTask)
        expect:
        info.is(taskClassInfoStore.getTaskClassInfo(SimpleTask))
    }

    def "simple task info"() {
        def info = taskClassInfoStore.getTaskClassInfo(SimpleTask)
        expect:
        !info.incremental
        info.taskActions.size() == 1
    }

    def "incremental task info"() {
        def info = taskClassInfoStore.getTaskClassInfo(IncrementalTask)
        expect:
        info.incremental
        info.taskActions.size() == 1
    }

    def "task with multiple actions"() {
        def info = taskClassInfoStore.getTaskClassInfo(MultipleActions)
        expect:
        !info.incremental
        info.taskActions.size() == 2
    }

    def "cannot have multiple incremental actions"() {
        when:
        taskClassInfoStore.getTaskClassInfo(MultipleIncrementalActions)
        then:
        def e = thrown(UncheckedExecutionException)
        e.cause.message == "Cannot have multiple @TaskAction methods accepting an IncrementalTaskInputs parameter."
    }

}
