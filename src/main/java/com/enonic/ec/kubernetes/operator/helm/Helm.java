package com.enonic.ec.kubernetes.operator.helm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

@Singleton
public class Helm
{
    private static Logger log = LoggerFactory.getLogger( Helm.class );

    private ObjectMapper objectMapper;

    @Inject
    public Helm( @Named("yaml") ObjectMapper objectMapper )
    {
        this.objectMapper = objectMapper;
    }

    public void install( Chart chart, Object values, String namespace, String name )
        throws IOException
    {
        String output = runCommandWithValuesFile( "install", chart.uri(), values, Optional.of( namespace ), name );
        Preconditions.checkState( output.contains( "STATUS: deployed" ) );
    }

    public void upgrade( Chart chart, Object values, String namespace, String name )
        throws IOException
    {
        String output = runCommandWithValuesFile( "upgrade", chart.uri(), values, Optional.of( namespace ), name );
        Preconditions.checkState( output.contains( "STATUS: deployed" ) );
    }

    public void rollback( String namespace, String name, Integer revision )
        throws IOException
    {
        String output = runCommand( new ProcessBuilder( "helm", "rollback", "-n", namespace, name, revision.toString() ) );
        Preconditions.checkState( output.contains( "Rollback was a success!" ) );
    }

    public String template( Chart chart, Object values )
        throws IOException
    {
        return runCommandWithValuesFile( "template", chart.uri(), values, Optional.empty(), "none" );
    }

    public List<HasMetadata> templateToObjects( Chart chart, Object values )
    {
        try
        {
            String res = template( chart, values );
            List<HasMetadata> list = new LinkedList<>();
            for ( String obj : res.split( "---" ) )
            {
                if ( !obj.trim().equals( "" ) )
                {
                    list.add( objectMapper.readValue( obj, HasMetadata.class ) );
                }
            }
            return list;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

    }

    public void uninstall( String namespace, String name )
        throws IOException
    {
        String output = runCommand( new ProcessBuilder( "helm", "uninstall", "-n", namespace, name ) );
        Preconditions.checkState( output.contains( "release \"" + name + "\" uninstalled" ) );
    }

    private String runCommand( ProcessBuilder pb )
        throws IOException
    {
        pb.redirectErrorStream( true );
        Process process = pb.start();
        try (BufferedReader outReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ))
        {
            int exit = process.waitFor();
            String output = String.join( System.lineSeparator(), outReader.lines().collect( Collectors.toList() ) );
            log.info( "Helm output:\n" + output );
            if ( exit != 0 )
            {
                throw new IOException( output );
            }
            return output;
        }
        catch ( InterruptedException e )
        {
            throw new IOException( e );
        }
    }

    private String runCommandWithValuesFile( String command, String chart, Object values, Optional<String> namespace, String name )
        throws IOException
    {
        File tmp = createValuesFile( values );
        try
        {
            List<String> cmd = new LinkedList<>( Arrays.asList( "helm", command, "-f", tmp.getAbsolutePath(), name, chart ) );
            namespace.ifPresent( ns -> {
                cmd.add( "-n" );
                cmd.add( ns );
            } );
            ProcessBuilder pb = new ProcessBuilder( cmd );
            return runCommand( pb );
        }
        finally
        {
            tmp.delete();
        }
    }

    private File createValuesFile( Object values )
        throws IOException
    {
        File tmp = File.createTempFile( "operator", ".yaml" );
        objectMapper.writeValue( tmp, values );
        return tmp;
    }
}
