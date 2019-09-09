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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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
        return Version.valueOf(project.getVersion());
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
        return repositoryBuilder
                .setGitDir(dotGitDirectory)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
    }

    /**
     * Changed project version
     * @param newVersion the new version.
     * @throws MojoExecutionException if the method fails.
     */
    void changeProjectVersion(Version newVersion) throws MojoExecutionException {
        changeProjectVersion(newVersion.toString());
    }

    /**
     * Changed project version
     * @param newVersion the new version.
     * @throws MojoExecutionException if the method fails.
     */
    void changeProjectVersion(String newVersion) throws MojoExecutionException {
        if (newVersion != null && !newVersion.equals(project.getVersion())) {
            try {
                Path path = project.getFile().toPath();
                String data = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLEventReader eventReader = factory.createXMLEventReader(new FileReader(project.getFile()));

                String xpath = "";
                boolean find = false;
                int begin = 0;
                while (eventReader.hasNext() && !find) {
                    XMLEvent xmlEvent = eventReader.nextEvent();
                    if (xmlEvent.isStartElement()) {
                        StartElement startElement = xmlEvent.asStartElement();
                        xpath = xpath + "/" + startElement.getName().getLocalPart();
                        if (PROJECT_VERSION_XPATH.equals(xpath)) {
                            begin = xmlEvent.getLocation().getCharacterOffset() + XML_VERSION_BEGIN_ELEMENT_LENGTH;
                        }
                    } else if (xmlEvent.isEndElement()) {
                        if (PROJECT_VERSION_XPATH.equals(xpath)) {
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
                    Files.write(path, data.getBytes(StandardCharsets.UTF_8));
                    getLog().info("Set project version: " + newVersion);
                }

            } catch (Exception ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        }
    }
}
