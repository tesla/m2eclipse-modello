/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.modello.internal;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelloBuildParticipant
    extends MojoExecutionBuildParticipant
{
    protected final Logger log = LoggerFactory.getLogger( getClass() );

    protected final ModelloProjectConfigurator projectConfigurator;

    public ModelloBuildParticipant( MojoExecution execution, ModelloProjectConfigurator projectConfigurator )
    {
        super( execution, true );
        this.projectConfigurator = projectConfigurator;
    }

    @Override
    public void clean( IProgressMonitor monitor )
        throws CoreException
    {
        super.clean( monitor );

        MavenProject mavenProject = getMavenProjectFacade().getMavenProject( monitor );
        IProject project = getMavenProjectFacade().getProject();
        File[] outputDirs = projectConfigurator.getOutputFolders( mavenProject, getMojoExecution(), monitor );
        for ( File outputDir : outputDirs )
        {
            log.debug( "Deleting directory members {}", outputDir.getAbsolutePath() );
            IFolder outputFolder = project.getFolder( getProjectRelativePath( project, outputDir ) );

            // JDT JavaBuilder considers added/removed optional source folders as classpath changes
            // keep folders (but not their content) to prevent build escalation

            if ( outputFolder.exists() )
            {
                outputFolder.refreshLocal( IResource.DEPTH_ONE, monitor );
                for ( IResource member : outputFolder.members() )
                {
                    member.delete( true /* force */, monitor );
                }
            }
            else
            {
                log.debug( "Directory {} does not exist", outputDir.getAbsolutePath() );
            }
        }
    }

    public static IPath getProjectRelativePath( IProject project, File file )
    {
        IPath projectPath = project.getLocation();
        IPath filePath = new Path( file.getAbsolutePath() );
        if ( !projectPath.isPrefixOf( filePath ) )
        {
            return null;
        }

        return filePath.removeFirstSegments( projectPath.segmentCount() );
    }
}
