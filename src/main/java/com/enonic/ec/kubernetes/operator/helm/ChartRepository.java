package com.enonic.ec.kubernetes.operator.helm;

public interface ChartRepository
{
    Chart get( String name );
}
