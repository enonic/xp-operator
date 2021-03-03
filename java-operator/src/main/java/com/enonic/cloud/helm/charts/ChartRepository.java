package com.enonic.cloud.helm.charts;

public interface ChartRepository
{
    Chart get( String name );
}
