package com.enonic.kubernetes.helm;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.helm.charts.Chart;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;


@SuppressWarnings("unused")
@Singleton
public class Helm
{
    private static final Logger log = LoggerFactory.getLogger( Helm.class );

    private static final String helmBin = "helm";

    private final ObjectMapper objectMapper;

    @Inject
    public Helm()
    {
        singletonAssert(this, "constructor");
        assertHelmVersion();
        this.objectMapper = new ObjectMapper( new YAMLFactory() );
    }

    private void assertHelmVersion()
    {
        ProcessBuilder pb = new ProcessBuilder( helmBin, "version" );
        try
        {
            String result = runCommand( pb );
            Preconditions.checkState( result.contains( "Version:\"v3" ), "Helm version not 3: " + result );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
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

    @SuppressWarnings("unused")
    public void rollback( String namespace, String name, Integer revision )
        throws IOException
    {
        String output = runCommand( new ProcessBuilder( helmBin, "rollback", "-n", namespace, name, revision.toString() ) );
        Preconditions.checkState( output.contains( "Rollback was a success!" ) );
    }

    @SuppressWarnings("WeakerAccess")
    public String template( Chart chart, Object values )
        throws IOException
    {
        return runCommandWithValuesFile( "template", chart.uri(), values, Optional.empty(), "none" );
    }

    @SuppressWarnings("WeakerAccess")
    public List<HasMetadata> templateObjects( Chart chart, Object values )
    {
        try
        {
            List<HasMetadata> res = new LinkedList<>();
            for ( String s : templateStrings( chart, values ) )
            {
                res.add( objectMapper.readValue( s, HasMetadata.class ) );
            }
            return res;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private List<String> templateStrings( Chart chart, Object values )
        throws IOException
    {
        String template = template( chart, values );
        String[] objects = template.split( "---" );
        List<String> res = new LinkedList<>();
        for ( String s : objects )
        {
            if ( s.contains( "apiVersion:" ) )
            {
                res.add( s );
            }
        }
        return res;
    }

    public void uninstall( String namespace, String name )
        throws IOException
    {
        String output = runCommand( new ProcessBuilder( helmBin, "uninstall", "-n", namespace, name ) );
        Preconditions.checkState( output.contains( "release \"" + name + "\" uninstalled" ) );
    }

    private String runCommand( ProcessBuilder pb )
        throws IOException
    {
        pb.redirectErrorStream( true );
        Process process = pb.start();
        try (BufferedReader outReader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ))
        {
            StringBuilder sb = new StringBuilder();
            String outputLine;
            while ((outputLine = outReader.readLine()) != null) {
                sb.append(outputLine).append('\n');
            }

            int exit = process.waitFor();
            String output = sb.toString();
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private String runCommandWithValuesFile( String command, String chart, Object values, Optional<String> namespace, String name )
        throws IOException
    {
        File tmp = createValuesFile( values );
        try
        {
            List<String> cmd = new LinkedList<>( Arrays.asList( helmBin, command, "-f", tmp.getAbsolutePath(), name, chart ) );
            namespace.ifPresent( ns -> {
                cmd.add( "-n" );
                cmd.add( ns );
            } );
            ProcessBuilder pb = new ProcessBuilder( cmd );
            return runCommand( pb );
        }
        finally
        {
            if ( !tmp.delete() )
            {
                log.warn( "Could not delete file: " + tmp.getAbsolutePath() );
            }
        }
    }

    private File createValuesFile( Object values )
        throws IOException
    {
        File tmp = File.createTempFile( "operator", ".yaml" );
        try
        {
            objectMapper.writeValue( tmp, values );
        }
        catch ( IOException e )
        {
            log.error( "Unable to create values file", e );
            throw e;
        }
        return tmp;
    }
}
