package com.enonic.cloud.common.functions;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

@Singleton
public class OptionalListPruner<O>
    implements Function<List<Optional<O>>, List<O>>
{
    @Override
    public List<O> apply( final List<Optional<O>> o )
    {
        return o.stream().
            filter( Optional::isPresent ).
            map( Optional::get ).
            collect( Collectors.toList() );
    }
}
