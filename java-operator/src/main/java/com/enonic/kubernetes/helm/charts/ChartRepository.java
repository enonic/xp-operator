package com.enonic.kubernetes.helm.charts;

public interface ChartRepository
{
    Chart get( String name );
}
