package com.enonic.cloud.operator.helm;

public interface ChartRepository
{
    Chart get( String name );
}
