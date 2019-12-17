package com.enonic.ec.kubernetes;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;

public class TestFileSupplier
{
    private void getFiles( List<File> hits, File file, String fileExtension, List<String> exclude )
    {

        if ( file.isDirectory() )
        {
            for ( File f : file.listFiles() )
            {
                getFiles( hits, f, fileExtension, exclude );
            }
        }
        else
        {
            if ( file.getName().endsWith( fileExtension ) && !exclude.contains( file.getName() ) )
            {
                hits.add( file );
            }
        }
    }

    private String getClassFilePath( Class k )
    {
        return k.getPackage().getName().replace( ".", "/" );
    }

    public List<File> getFiles( Class k, String fileExtension, String... exclude )
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource( getClassFilePath( k ) );
        String path = url.getPath();
        List<File> res = new LinkedList<>();
        getFiles( res, new File( path ), fileExtension, Arrays.asList( exclude ) );
        return res;
    }

    public File getFile( Class k, String file )
    {
        URL resource = k.getResource( file );
        Assertions.assertNotNull( resource, "File " + file + " not found" );
        return new File( resource.getPath() );
    }

    private String testName( File file )
    {
        return file.getName();
    }

    public Stream<DynamicTest> createTests( Class k, Consumer<File> consumer, String... exclude )
    {
        String classResourcePath = getClassFilePath( k );
        List<DynamicTest> tests = new LinkedList<>();
        for ( File f : getFiles( k, ".yaml", exclude ) )
        {
            String[] name = f.getAbsolutePath().split( classResourcePath );
            tests.add( DynamicTest.dynamicTest( name[name.length - 1], f.toURI(), () -> consumer.accept( f ) ) );
        }
        return tests.stream();
    }
}
