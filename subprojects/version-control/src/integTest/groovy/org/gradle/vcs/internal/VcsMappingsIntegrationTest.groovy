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

package org.gradle.vcs.internal

class VcsMappingsIntegrationTest extends AbstractVcsIntegrationTest {
    def setup() {
        settingsFile << """
            import ${DirectoryRepository.canonicalName}
        """
    }

    def "can define and use source repositories"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepository) {
                            sourceDir = file("dep")
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        file("build/vcs/dep/checkedout").assertIsFile()
    }

    def "can define and use source repositories with all {}"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    addRule("rule") { details ->
                        if (details.requested.group == "org.test") {
                            from vcs(DirectoryRepository) {
                                sourceDir = file("dep")
                            }
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        file("build/vcs/dep/checkedout").assertIsFile()
    }

    def "can define unused vcs mappings"() {
        settingsFile << """
            // include the missing dep as a composite
            includeBuild 'dep'
            
            sourceControl {
                vcsMappings {
                    withModule("unused:dep") {
                        from vcs(DirectoryRepository) {
                            sourceDir = file("does-not-exist")
                        }
                    }
                    addRule("rule") { details ->
                        if (details.requested.group == "unused") {
                            from vcs(DirectoryRepository) {
                                sourceDir = file("does-not-exist")
                            }
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        file("build/vcs/dep/checkedout").assertDoesNotExist()
        file("build/vcs/does-not-exist/checkedout").assertDoesNotExist()
    }

    def "last vcs mapping rule wins"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepository) {
                            sourceDir = file("does-not-exist")
                        }
                    }
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepository) {
                            sourceDir = file("dep")
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        file("build/vcs/dep/checkedout").assertIsFile()
        file("build/vcs/does-not-exist/checkedout").assertDoesNotExist()
    }
}
