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

package org.apache.axis2.maven2.mar;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;



/**
 * Abstract base class of all the mojos in the axis2-mar-maven-plugin.
 */
public abstract class AbstractMarMojo
    extends AbstractMojo
{

    /**
     * The projects base directory.
     *
     * @parameter expression="${project.basedir}"
     * @required
     * @readonly
     */
    protected File baseDir;
    
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    /**
     * The directory where the mar is built.
     *
     * @parameter expression="${project.build.directory}/mar"
     * @required
     */
    protected File marDirectory;

    /**
     * The location of the module.xml file.  If it is present in the META-INF
     * directory in src/main/resources with that name then it will automatically be 
     * included. Otherwise this parameter must be set.
     *
     * @parameter 
     */
    private File moduleXmlFile;

    /**
     * Additional file sets, which are being added to the archive.
     *
     * @parameter
     */
    private FileSet[] fileSets;

    /**
     * Whether the dependency jars should be included in the mar
     * 
     * @parameter expression="${includeDependencies}" default-value="true"
     */
    private boolean includeDependencies;
    
    /**
     * Builds the exploded mar file.
     * @throws MojoExecutionException
     */
    protected void buildExplodedMar( )
        throws MojoExecutionException
    {
        getLog().debug( "Exploding mar..." );

        marDirectory.mkdirs();
        getLog().debug( "Assembling mar " + project.getArtifactId() + " in " + marDirectory );

        try
        {
            final File metaInfDir = new File( marDirectory, "META-INF" );
            final File libDir = new File(marDirectory, "lib");
            final File moduleFileTarget = new File( metaInfDir, "module.xml" );
            boolean existsBeforeCopyingClasses = moduleFileTarget.exists();

            if ( classesDirectory.exists() && ( !classesDirectory.equals( marDirectory ) ) )
            {
                FileUtils.copyDirectoryStructure( classesDirectory, marDirectory );
            }

            if ( fileSets != null )
            {
                for ( int i = 0;  i < fileSets.length;   i++ )
                {
                    FileSet fileSet = fileSets[i];
                    copyFileSet( fileSet, marDirectory );
                }
            }
            
            copyMetaInfFile( moduleXmlFile, moduleFileTarget, existsBeforeCopyingClasses, "module.xml file" );

            if(includeDependencies){
                Set<Artifact> artifacts = project.getArtifacts();
    
                List<String> duplicates = findDuplicates( artifacts );
    
                for (Artifact artifact : artifacts)
                {
                	String targetFileName = getDefaultFinalName( artifact );
    
                	getLog().debug( "Processing: " + targetFileName );
    
                	if ( duplicates.contains( targetFileName ) )
                	{
                		getLog().debug( "Duplicate found: " + targetFileName );
                		targetFileName = artifact.getGroupId() + "-" + targetFileName;
                		getLog().debug( "Renamed to: " + targetFileName );
                	}
    
                	// TODO: utilise appropriate methods from project builder
                	ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
                	if (filter.include( artifact ) )
                	{
                		String type = artifact.getType();
                		if ( "jar".equals( type ) )
                		{
                			copyFileIfModified( artifact.getFile(), new File( libDir, targetFileName ) );
                		}
                	}
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode mar...", e );
        }
    }

    /**
     * Searches a set of artifacts for duplicate filenames and returns a list of duplicates.
     *
     * @param artifacts set of artifacts
     * @return List of duplicated artifacts
     */
    private List<String> findDuplicates( Set<Artifact> artifacts )
    {
        List<String> duplicates = new ArrayList<String>();
        List<String> identifiers = new ArrayList<String>();
        for (Artifact artifact : artifacts)
        {
            String candidate = getDefaultFinalName( artifact );
            if ( identifiers.contains( candidate ) )
            {
                duplicates.add( candidate );
            }
            else
            {
                identifiers.add( candidate );
            }
        }
        return duplicates;
    }

    /**
     * Converts the filename of an artifact to artifactId-version.type format.
     *
     * @param artifact
     * @return converted filename of the artifact
     */
    private String getDefaultFinalName( Artifact artifact )
    {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + "." +
            artifact.getArtifactHandler().getExtension();
    }

    /**
     * Copy file from source to destination only if source timestamp is later than the destination timestamp.
     * The directories up to <code>destination</code> will be created if they don't already exist.
     * <code>destination</code> will be overwritten if it already exists.
     *
     * @param source      An existing non-directory <code>File</code> to copy bytes from.
     * @param destination A non-directory <code>File</code> to write bytes to (possibly
     *                    overwriting).
     * @throws IOException                   if <code>source</code> does not exist, <code>destination</code> cannot be
     *                                       written to, or an IO error occurs during copying.
     * @throws java.io.FileNotFoundException if <code>destination</code> is a directory
     *                                       <p/>
     *                                       TO DO: Remove this method when Maven moves to plexus-utils version 1.4
     */
    private void copyFileIfModified( File source, File destination )
        throws IOException
    {
        // TO DO: Remove this method and use the method in WarFileUtils when Maven 2 changes
        // to plexus-utils 1.2.
        if ( destination.lastModified() < source.lastModified() )
        {
            FileUtils.copyFile( source.getCanonicalFile(), destination );
            // preserve timestamp
            destination.setLastModified( source.lastModified() );
        }
    }

    private void copyFileSet( FileSet fileSet, File targetDirectory )
        throws IOException
    {
        File dir = fileSet.getDirectory();
        if ( dir == null )
        {
            dir = baseDir;
        }
        File targetDir = targetDirectory;
        if ( fileSet.getOutputDirectory() != null )
        {
            targetDir = new File( targetDir, fileSet.getOutputDirectory() );
        }
        if ( targetDir.equals( dir ) )
        {
            return;
        }

        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir( dir );
        if ( !fileSet.isSkipDefaultExcludes() )
        {
            ds.addDefaultExcludes();
        }
        final String[] excludes = fileSet.getExcludes();
        if ( excludes != null )
        {
            ds.setExcludes( excludes );
        }
        final String[] includes = fileSet.getIncludes();
        if ( includes != null )
        {
            ds.setIncludes( includes );
        }
        ds.scan();
        String[] files = ds.getIncludedFiles();
        for ( int i = 0;  i < files.length;  i++ )
        {
            File sourceFile = new File( dir, files[i] );
            File targetFile = new File( targetDir, files[i] );
            FileUtils.copyFile( sourceFile, targetFile );
        }
    }


    private void copyMetaInfFile( final File pSource, final File pTarget,
                                  final boolean pExistsBeforeCopying,
                                  final String pDescription )
        throws MojoExecutionException, IOException
    {
        if ( pSource != null  &&  pTarget != null )
        {
            if ( !pSource.exists() )
            {
                throw new MojoExecutionException( "The configured " + pDescription + " could not be found at "
                                                  + pSource );
            }

            if ( !pExistsBeforeCopying  &&  pTarget.exists() )
            {
                getLog().warn( "The configured " + pDescription + " overwrites another file from the classpath." );
            }

            FileUtils.copyFile( pSource, pTarget );
        }
    }
}
