package com.enonic.ec.kubernetes.operator.helm;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class JarRepository
    extends LocalRepository
{
    public JarRepository( String resource )
    {
        super( copyToTmpFolder( resource ) );
    }

    private static Path copyToTmpFolder( final String resource )
    {
        try
        {
            File f = File.createTempFile( "operator", "charts" );
            f.mkdir();
            Path chartsPath = f.toPath();

            URL r = Thread.currentThread().getContextClassLoader().getResource( resource );
            copyFolder( new File( r.toURI() ).toPath(), chartsPath );

            return chartsPath;
        }
        catch ( IOException | URISyntaxException e )
        {
            throw new RuntimeException( e );
        }

    }

    private static void copyFolder( Path src, Path dest )
        throws IOException
    {
        Files.walk( src ).forEach( source -> copy( source, dest.resolve( src.relativize( source ) ) ) );
    }

    private static void copy( Path source, Path dest )
    {
        try
        {
            Files.copy( source, dest, REPLACE_EXISTING );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }
}
