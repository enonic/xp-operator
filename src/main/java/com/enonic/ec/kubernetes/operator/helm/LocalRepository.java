package com.enonic.ec.kubernetes.operator.helm;

import java.io.File;
import java.nio.file.Path;

import com.google.common.base.Preconditions;

public class LocalRepository
    implements ChartRepository
{
    private File path;

    public LocalRepository( File path )
    {
        this.path = path;
        Preconditions.checkState( path.isDirectory(), "path must be a directory" );
    }

    @Override
    public Chart get( final String name )
    {
        return ImmutableChart.builder().
            uri( path.getAbsolutePath() + "/" + name ).
            build();
    }
}
