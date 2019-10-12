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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

@Mojo(name = "release-create",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresProject = true,
        threadSafe = true)
public class ReleaseMojo extends AbstractSemVerMojo {

    /**
     * Skip git push to remote repository.
     */
    @Parameter(property = "skipPush", name = "skipPush", defaultValue = "false")
    boolean skipPush;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Version version = getVersion();
        Version newVersion;
        if (version.getPatchVersion() == 0) {
            newVersion = version.incrementMinorVersion(SNAPSHOT);
        } else {
            newVersion = version.incrementPatchVersion(SNAPSHOT);
        }

        try (Repository repository = getGitRepository()) {
            try (Git git = new Git(repository)) {

                String tag = version.getNormalVersion();
                git.tag().setName(tag).call();
                getLog().info("Create tag: " + tag);

                changeProjectVersion(newVersion.toString());

                git.add().setUpdate(true).addFilepattern(".").call();
                git.commit().setMessage("Development version " + newVersion).call();
                if (!skipPush) {
                    git.push().setRemote("origin").setPushTags().setPushAll().call();
                    getLog().info("Git push changes.");
                } else {
                    getLog().info("Git push disabled!");
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

}
