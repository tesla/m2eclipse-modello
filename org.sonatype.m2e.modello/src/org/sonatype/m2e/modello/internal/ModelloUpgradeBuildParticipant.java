/*******************************************************************************
 * Copyright (c) 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.sonatype.m2e.modello.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * As of version 1.1, modello-upgrade plugin does not properly participate in m2e workspace build, i.e. it neither
 * checks for model file changes nor communicates generated files back to m2e. As a result, projects using
 * modello-upgrade plugin are very prone to endless builds. This build participant implements required workspace
 * integration "around" existing modello-upgrade-plugin execution.
 */
public class ModelloUpgradeBuildParticipant
    extends ModelloBuildParticipant
{

    public ModelloUpgradeBuildParticipant( MojoExecution execution, ModelloProjectConfigurator projectConfigurator )
    {
        super( execution, projectConfigurator );
    }

    @Override
    public Set<IProject> build( int kind, IProgressMonitor monitor )
        throws Exception
    {
        MavenProject mavenProject = getMavenProjectFacade().getMavenProject( monitor );

        if ( !hasModelDelta( mavenProject, monitor ) )
        {
            return Collections.emptySet();
        }

        Set<IProject> result = super.build( kind, monitor );

        File[] outputFolders = projectConfigurator.getOutputFolders( mavenProject, getMojoExecution(), monitor );
        for ( File outputFolder : outputFolders )
        {
            getBuildContext().refresh( outputFolder );
        }

        return result;
    }

    private boolean hasModelDelta( MavenProject project, IProgressMonitor monitor )
        throws CoreException
    {
        List<String> results = new ArrayList<String>();
        String model =
            projectConfigurator.getParameterValue( project, "model", String.class, getMojoExecution(), monitor );
        if ( model != null )
        {
            results.add( model );
        }
        String[] models =
            projectConfigurator.getParameterValue( project, "models", String[].class, getMojoExecution(), monitor );
        if ( models != null && models.length > 0 )
        {
            results.addAll( Arrays.asList( models ) );
        }

        return getBuildContext().hasDelta( results );
    }
}
