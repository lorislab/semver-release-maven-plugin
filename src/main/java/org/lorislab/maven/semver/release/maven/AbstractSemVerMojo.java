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
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.ordering.ReactorDepthComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The abstract semver mojo.
 */
abstract class AbstractSemVerMojo extends AbstractMojo {

    /**
     * The snapshot suffix
     */
    static final String SNAPSHOT = "SNAPSHOT";

    /**
     * The project version xpath
     */
    private static final String PROJECT_VERSION_XPATH = "/project/version";

    /**
     * The project parent version xpath
     */
    private static final String PARENT_PROJECT_VERSION_XPATH = "/project/parent/version";

    /**
     * The project version start element.
     */
    private static final int XML_VERSION_BEGIN_ELEMENT_LENGTH = "<version>".length();

    /**
     * The project git directory
     */
    @Parameter(defaultValue = "${project.basedir}/.git")
    File dotGitDirectory;

    /**
     * The maven project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Get maven project version.
     *
     * @return the maven project version.
     */
    Version getVersion() {
        try {
            return Version.valueOf(project.getVersion());
        } catch (Exception ex) {
            getLog().error("Only the SemVer 2.0 format (major.minor.patch-label) is supported for the project version!");
            getLog().error("Error parsing version: " + project.getVersion() + ", error:" + ex.toString());
            throw ex;
        }
    }

    /**
     * Returns {@code true} if the pre release version is SNAPSHOT.
     *
     * @param version the version.
     * @return {@code true} if the pre release version is SNAPSHOT.
     */
    boolean isSnapshot(Version version) {
        return SNAPSHOT.equals(version.getPreReleaseVersion());
    }

    /**
     * Gets the git repository.
     *
     * @return the git repository.
     * @throws IOException if the method fails.
     */
    Repository getGitRepository() throws IOException {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        if (dotGitDirectory.exists() && dotGitDirectory.isDirectory()) {
            repositoryBuilder.setGitDir(dotGitDirectory);
        }
        return repositoryBuilder.readEnvironment() // scan environment GIT_* variables
                // .findGitDir() // scan up the file system tree
                .build();
    }

    /**
     * Changed project version
     *
     * @param newVersion the new version.
     * @throws MojoExecutionException if the method fails.
     */
    void changeProjectVersion(String newVersion) throws MojoExecutionException {
        // change current project
        if (newVersion != null && !newVersion.equals(project.getVersion())) {

            // update parent in the project children
            try {
                Map<String, Model> reactorModels = PomHelper.getReactorModels(project, getLog());
                final SortedMap<String, Model> reactor = new TreeMap<String, Model>(
                        new ReactorDepthComparator(reactorModels));
                reactor.putAll(reactorModels);

                changeVersions("", reactor, project.getGroupId(), project.getArtifactId(), newVersion);
            } catch (Exception ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        }
    }

    private void changeVersions(String key, SortedMap<String, Model> reactor, String groupId, String artifactId,
            String newVersion) throws MojoExecutionException {
        // change the current pom version
        changeVersion(getFile(project, key), newVersion, PROJECT_VERSION_XPATH);

        // change the current pom child parent version
        Map<String, Model> children = PomHelper.getChildModels(reactor, groupId, artifactId);
        for (Map.Entry<String, Model> child : children.entrySet()) {
            File file = getFile(project, child.getKey());
            changeVersion(file, newVersion, PARENT_PROJECT_VERSION_XPATH);
            String childGroupId = child.getValue().getGroupId();
            if (childGroupId == null) {
                childGroupId = groupId;
            }
            // change the child version
            changeVersions(child.getKey(), reactor, childGroupId, child.getValue().getArtifactId(), newVersion);
        }
    }

    private void changeVersion(File file, String newVersion, String xp) throws MojoExecutionException {
        try {
            String data = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(file));
            String xpath = "";
            boolean find = false;
            int begin = 0;
            while (eventReader.hasNext() && !find) {
                XMLEvent xmlEvent = eventReader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    StartElement startElement = xmlEvent.asStartElement();
                    xpath = xpath + "/" + startElement.getName().getLocalPart();
                    if (xp.equals(xpath)) {
                        begin = xmlEvent.getLocation().getCharacterOffset() + XML_VERSION_BEGIN_ELEMENT_LENGTH;
                    }
                } else if (xmlEvent.isEndElement()) {
                    if (xp.equals(xpath)) {
                        int end = xmlEvent.getLocation().getCharacterOffset();
                        find = true;
                        data = data.substring(0, begin) + newVersion + data.substring(end);
                    } else {
                        int index = xpath.lastIndexOf("/");
                        if (index > -1) {
                            xpath = xpath.substring(0, index);
                        }
                    }
                }
            }

            if (find) {
                Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
                getLog().info("Set " + xp + " to " + newVersion + " in file " + file);
            }

        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private File getFile(MavenProject project, String relativePath) {
        final File moduleDir = new File(project.getBasedir(), relativePath);
        final File projectBaseDir = project.getBasedir();
        if (projectBaseDir.equals(moduleDir)) {
            return project.getFile();
        } else if (moduleDir.isDirectory()) {
            return new File(moduleDir, "pom.xml");
        }
        return moduleDir;
    }
}
