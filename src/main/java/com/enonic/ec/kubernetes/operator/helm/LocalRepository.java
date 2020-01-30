package com.enonic.ec.kubernetes.operator.helm;

import java.io.File;

import com.google.common.base.Preconditions;

public class LocalRepository
    implements ChartRepository
{
    private final File path;

    public LocalRepository( File path )
    {
        this.path = path;
        Preconditions.checkState( path.isDirectory(), "path must be a directory" );
    }

    @SuppressWarnings("unused")
    @Override
    public Chart get( final String name )
    {
        return ImmutableChart.builder().
            uri( path.getAbsolutePath() + "/" + name ).
            build();
    }
}
