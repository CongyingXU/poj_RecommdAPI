/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.maven2.aar;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;

/**
 * Build a aar.
 *
 * @goal aar
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class AarMojo extends AbstractAarMojo {
    /**
     * The Maven Session
     *
     * @required
     * @readonly
     * @parameter expression="${session}"
     */
    private MavenSession session;
    
    /**
     * The directory for the generated aar.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String outputDirectory;

    /**
     * The name of the generated aar.
     *
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String aarName;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @required
     */
    private JarArchiver jarArchiver;

    /**
     * The maven archive configuration to use.
     *
     * @parameter
     */
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment
     * instead.
     *
     * @parameter
     */
    private String classifier;

    /**
     * Whether this is the main artifact being built. Set to <code>false</code> if you don't want to
     * install or deploy it to the local repository instead of the default one in an execution.
     *
     * @parameter expression="${primaryArtifact}" default-value="true"
     */
    private boolean primaryArtifact;

    /** @component */
    private MavenProjectHelper projectHelper;

    /**
     * Executes the AarMojo on the current project.
     *
     * @throws MojoExecutionException if an error occured while building the webapp
     */
    public void execute() throws MojoExecutionException {

        File aarFile = new File(outputDirectory, aarName + ".aar");

        try {
            performPackaging(aarFile);
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error assembling aar", e);
        }
    }

    /**
     * Generates the aar.
     *
     * @param aarFile the target aar file
     * @throws IOException
     * @throws ArchiverException
     * @throws ManifestException
     * @throws DependencyResolutionRequiredException
     *
     */
    private void performPackaging(File aarFile)
            throws IOException, ArchiverException, ManifestException,
            DependencyResolutionRequiredException,
            MojoExecutionException {

        buildExplodedAar();

        // generate aar file
        getLog().info("Generating aar " + aarFile.getAbsolutePath());
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(aarFile);
        jarArchiver.addDirectory(aarDirectory);

        // create archive
        archiver.createArchive(session, project, archive);

        if (classifier != null) {
            projectHelper.attachArtifact(project, "aar", classifier, aarFile);
        } else {
            Artifact artifact = project.getArtifact();
            if (primaryArtifact) {
                artifact.setFile(aarFile);
            } else if (artifact.getFile() == null || artifact.getFile().isDirectory()) {
                artifact.setFile(aarFile);
            } else {
                projectHelper.attachArtifact(project, "aar", aarFile);
            }
        }
    }
}
