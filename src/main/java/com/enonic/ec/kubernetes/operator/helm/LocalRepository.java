package com.enonic.ec.kubernetes.operator.helm;

import java.nio.file.Path;

import com.google.common.base.Preconditions;

public class LocalRepository
    implements ChartRepository
{
    private Path path;

    public LocalRepository( Path path )
    {
        this.path = path;
        Preconditions.checkState( path.toFile().isDirectory(), "path must be a directory" );
    }

    @Override
    public Chart get( final String name )
    {
        return ImmutableChart.builder().
            uri( path.toString() + "/" + name ).
            build();
    }
}
