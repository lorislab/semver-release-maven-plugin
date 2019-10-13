/*
 * Copyright 2019 lorislab.org.
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

package org.lorislab.maven.semver.release.maven;

import com.github.zafarkhaja.semver.Version;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Change the project version to git hash pre release.
 */
@Mojo(name = "version-git-hash",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresProject = true,
        aggregator = true,
        threadSafe = true)
public class VersionGitHashMojo extends AbstractSemVerMojo {

    /**
     * The git hash length.
     */
    @Parameter(defaultValue = "7")
    int abbrevLength;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Version version = getVersion();
        if (isSnapshot(version)) {

            // replace SNAPSHOT with git hash
            String tmp = gitHash();
            version = version.setPreReleaseVersion(tmp);

            // change project version
            changeProjectVersion(version.toString());
        }
    }

    /**
     * Gets the current git hash.
     *
     * @return the git hash.
     * @throws MojoExecutionException if the method fails.
     */
    private String gitHash() throws MojoExecutionException {
        try (Repository repository = getGitRepository()) {
            Ref head = repository.findRef("HEAD");
            try (ObjectReader objectReader = repository.newObjectReader()) {
                AbbreviatedObjectId abbreviatedObjectId = objectReader.abbreviate(head.getObjectId(), abbrevLength);
                return abbreviatedObjectId.name();
            }
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

}
