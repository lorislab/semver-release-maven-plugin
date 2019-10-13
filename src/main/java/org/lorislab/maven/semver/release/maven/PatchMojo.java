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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.text.MessageFormat;

/**
 * Creates the patch.
 */
@Mojo(name = "patch-create",
        defaultPhase = LifecyclePhase.INITIALIZE,
        requiresProject = true,
        aggregator = true,
        threadSafe = true)
public class PatchMojo extends AbstractSemVerMojo {

    /**
     * Component used to prompt for input
     */
    @Component
    private Prompter prompter;

    /**
     * The patch version.
     */
    @Parameter(property = "patchVersion", name = "patchVersion")
    String patchVersion;

    /**
     * The patch version. Default is '{0}.{1}'.
     * {0} - major version
     * {1} - minor version.
     */
    @Parameter(property = "branchPattern", name = "branchPattern", defaultValue = "{0}.{1}")
    String branchPattern;

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

        if (patchVersion == null) {
            try {
                patchVersion = prompter.prompt("Enter the patch version ");
            } catch (PrompterException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        Version version = Version.valueOf(patchVersion);
        if (version.getPatchVersion() != 0) {
            throw new MojoExecutionException("The version does not have patch 0");
        }

        Version releaseVersion = Version.valueOf(version.getNormalVersion());
        Version pv = releaseVersion.incrementPatchVersion(SNAPSHOT);

        try (Repository repository = getGitRepository()) {
            try (Git git = new Git(repository)) {

                String branch = repository.getBranch();

                // the branch name
                String branchName = MessageFormat.format(branchPattern, releaseVersion.getMajorVersion(), releaseVersion.getMinorVersion());

                // checkout TAG release as patch branch
                Ref ref = repository.findRef(releaseVersion.getNormalVersion());
                RevWalk revWalk = new RevWalk(repository);
                RevCommit tag = revWalk.parseCommit(ref.getObjectId());
                git.checkout().setCreateBranch(true).setName(branchName).setStartPoint(tag).call();
                getLog().info("Create new branch: " + branchName);

                // change to patch version
                changeProjectVersion(pv.toString());

                // commit changes
                git.add().setUpdate(true).addFilepattern(".").call();
                git.commit().setMessage("Create patch version " + pv).call();
                if (!skipPush) {
                    git.push().setRemote("origin").setPushAll().call();
                    getLog().info("Git push changes.");
                } else {
                    getLog().info("Git push disabled!");
                }

                git.checkout().setName(branch).call();
            }
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        getLog().info("Create new patch version " + pv);
    }
}
