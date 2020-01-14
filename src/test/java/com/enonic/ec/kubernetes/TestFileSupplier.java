package com.enonic.ec.kubernetes;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

    protected String getClassFilePath( Class k )
    {
        return k.getPackage().getName().replace( ".", "/" );
    }

    public List<File> getFiles( Class k, String fileExtension, String... exclude )
    {
        List<File> res = new LinkedList<>();
        getFiles( res, Paths.get( "src/test/resources", getClassFilePath( k ) ).toFile(), fileExtension, Arrays.asList( exclude ) );
        return res;
    }

    public File getFile( Class k, String file )
    {
        return Paths.get( "src/test/resources", getClassFilePath( k ), file ).toFile();
    }

    protected String testName( String classResourcePath, File file )
    {
        String[] name = file.getAbsolutePath().split( classResourcePath );
        return name[name.length - 1];
    }

    public Stream<DynamicTest> createTests( Class k, Consumer<File> consumer, String... exclude )
    {
        String classResourcePath = getClassFilePath( k );
        List<DynamicTest> tests = new LinkedList<>();
        for ( File f : getFiles( k, ".yaml", exclude ) )
        {
            tests.add( DynamicTest.dynamicTest( testName( classResourcePath, f ), f.toURI(), () -> consumer.accept( f ) ) );
        }
        return tests.stream();
    }
}
