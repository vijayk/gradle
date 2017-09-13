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

package org.gradle.api.internal.artifacts.vcs;

import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.SourceComponentIdentifier;
import org.gradle.api.artifacts.component.SourceComponentSelector;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.component.DefaultBuildIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ComponentResolvers;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactSet;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.ModuleExclusion;
import org.gradle.api.internal.artifacts.type.ArtifactTypeRegistry;
import org.gradle.api.internal.component.ArtifactType;
import org.gradle.api.internal.file.TemporaryFileProvider;
import org.gradle.internal.component.model.ComponentArtifactMetadata;
import org.gradle.internal.component.model.ComponentOverrideMetadata;
import org.gradle.internal.component.model.ComponentResolveMetadata;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.internal.hash.HashUtil;
import org.gradle.internal.resolve.ModuleVersionResolveException;
import org.gradle.internal.resolve.resolver.ArtifactResolver;
import org.gradle.internal.resolve.resolver.ComponentMetaDataResolver;
import org.gradle.internal.resolve.resolver.DependencyToComponentIdResolver;
import org.gradle.internal.resolve.resolver.OriginArtifactSelector;
import org.gradle.internal.resolve.result.BuildableArtifactResolveResult;
import org.gradle.internal.resolve.result.BuildableArtifactSetResolveResult;
import org.gradle.internal.resolve.result.BuildableComponentIdResolveResult;
import org.gradle.internal.resolve.result.BuildableComponentResolveResult;
import org.gradle.vcs.VersionControlSpec;
import org.gradle.vcs.VersionControlSystem;
import org.gradle.vcs.internal.VcsMappingFactory;
import org.gradle.vcs.internal.VcsMappingInternal;
import org.gradle.vcs.internal.VcsMappingsInternal;
import org.gradle.vcs.internal.VersionControlSystemFactory;

import javax.annotation.Nullable;
import java.io.File;

public class VcsDependencyResolver implements ComponentMetaDataResolver, DependencyToComponentIdResolver, ArtifactResolver, OriginArtifactSelector, ComponentResolvers {
    private final VcsMappingsInternal vcsMappingsInternal;
    private final VcsMappingFactory vcsMappingFactory;
    private final VersionControlSystemFactory versionControlSystemFactory;
    private final TemporaryFileProvider temporaryFileProvider;

    public VcsDependencyResolver(VcsMappingsInternal vcsMappingsInternal, VcsMappingFactory vcsMappingFactory, VersionControlSystemFactory versionControlSystemFactory, TemporaryFileProvider temporaryFileProvider) {
        this.vcsMappingsInternal = vcsMappingsInternal;
        this.vcsMappingFactory = vcsMappingFactory;
        this.versionControlSystemFactory = versionControlSystemFactory;
        this.temporaryFileProvider = temporaryFileProvider;
    }

    @Override
    public DependencyToComponentIdResolver getComponentIdResolver() {
        return this;
    }

    @Override
    public ComponentMetaDataResolver getComponentResolver() {
        return this;
    }

    @Override
    public OriginArtifactSelector getArtifactSelector() {
        return this;
    }

    @Override
    public ArtifactResolver getArtifactResolver() {
        return this;
    }

    @Override
    public void resolve(DependencyMetadata dependency, BuildableComponentIdResolveResult result) {
        if (dependency.getSelector() instanceof SourceComponentSelector) {
            // TODO: Handle selectors that have already been created by some other means
//            ProjectComponentSelector selector = (ProjectComponentSelector) dependency.getSelector();
//            ProjectComponentIdentifier project = componentIdentifierFactory.createProjectComponentIdentifier(selector);
//            LocalComponentMetadata componentMetaData = localComponentRegistry.getComponent(project);
//            if (componentMetaData == null) {
//                result.failed(new ModuleVersionResolveException(selector, project + " not found."));
//            } else {
//                result.resolved(componentMetaData);
//            }
        } else {
            VcsMappingInternal vcsMappingInternal = getVcsMapping(dependency);
            if (vcsMappingInternal != null) {
                // TODO: Hardcoded version of "master"
                // This should be based on something from the repository.
                // TODO: Need to clone the repository now and get the version information?
                // TODO: Need failure handling, e.g., cannot clone repository
                vcsMappingsInternal.getVcsMappingRule().execute(vcsMappingInternal);
                if (vcsMappingInternal.isUpdated()) {
                    String version = "master";
                    String projectPath = ":"; // assume it's from the root
                    String buildName = vcsMappingInternal.getOldRequested().getName();
                    result.resolved(DefaultSourceComponentIdentifier.newIdentifier(vcsMappingInternal.getRepository(), projectPath, new DefaultBuildIdentifier(buildName)),
                        new DefaultModuleVersionIdentifier(vcsMappingInternal.getOldRequested().getGroup(), vcsMappingInternal.getOldRequested().getName(), version));
                }
            }
        }
    }

    private VcsMappingInternal getVcsMapping(DependencyMetadata dependency) {
        if (vcsMappingsInternal.hasRules()) {
            return vcsMappingFactory.create(dependency.getSelector(), dependency.getRequested());
        }
        return null;
    }

    @Override
    public void resolve(ComponentIdentifier identifier, ComponentOverrideMetadata componentOverrideMetadata, BuildableComponentResolveResult result) {
        if (isSourceComponentId(identifier)) {
            SourceComponentIdentifier sourceComponentIdentifier = (SourceComponentIdentifier) identifier;
            VersionControlSpec spec = sourceComponentIdentifier.getRepository();
            VersionControlSystem versionControlSystem = versionControlSystemFactory.create(spec);
            // TODO: We need to manage these working directories so they're shared across projects within a build (if possible)
            // and have some sort of global cache of cloned repositories
            File workingDir = temporaryFileProvider.newTemporaryFile("vcs", HashUtil.createCompactMD5(spec.getDisplayName()), sourceComponentIdentifier.getBuild().getName());
            versionControlSystem.populate(workingDir, spec);

            result.failed(new ModuleVersionResolveException(DefaultSourceComponentSelector.newSelector(spec, sourceComponentIdentifier.getProjectPath(), sourceComponentIdentifier.getBuild().getName()), identifier + " not supported yet."));
            // TODO: Populate component registry and implicitly included builds somehow
            // This is what ProjectDependencyResolver does:
//            LocalComponentMetadata componentMetaData = localComponentRegistry.getComponent(projectId);
//            if (componentMetaData == null) {
//                result.failed(new ModuleVersionResolveException(DefaultProjectComponentSelector.newSelector(projectId), projectId + " not found."));
//            } else {
//                result.resolved(componentMetaData);
//            }
        }
    }

    @Override
    public boolean isFetchingMetadataCheap(ComponentIdentifier identifier) {
        return !isSourceComponentId(identifier);
    }

    @Override
    public void resolveArtifactsWithType(ComponentResolveMetadata component, ArtifactType artifactType, BuildableArtifactSetResolveResult result) {
        if (isSourceComponentId(component.getComponentId())) {
            throw new UnsupportedOperationException("Resolving artifacts by type is not yet supported for source modules");
        }
    }

    @Nullable
    @Override
    public ArtifactSet resolveArtifacts(ComponentResolveMetadata component, ConfigurationMetadata configuration, ArtifactTypeRegistry artifactTypeRegistry, ModuleExclusion exclusions) {
        if (isSourceComponentId(component.getComponentId())) {
            // TODO: Need to resolve artifacts from built projects
            throw new UnsupportedOperationException("Resolving artifacts is not yet supported for source modules");
        } else {
            return null;
        }
    }

    @Override
    public void resolveArtifact(ComponentArtifactMetadata artifact, ModuleSource moduleSource, BuildableArtifactResolveResult result) {
        if (isSourceComponentId(artifact.getComponentId())) {
            // TODO: Need to resolve artifacts from built projects
            // This is what ProjectDependencyResolver does
//            LocalComponentArtifactMetadata projectArtifact = (LocalComponentArtifactMetadata) artifact;
//
//            File localArtifactFile = projectArtifact.getFile();
//            if (localArtifactFile != null) {
//                result.resolved(localArtifactFile);
//            } else {
//                result.notFound(projectArtifact.getId());
//            }
        }
    }

    private boolean isSourceComponentId(ComponentIdentifier componentId) {
        return componentId instanceof SourceComponentIdentifier;
    }
}
