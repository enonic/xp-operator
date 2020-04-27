package com.enonic.cloud.operator.common.info;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

public abstract class Diff<T>
{
    public abstract Optional<T> oldValue();

    public abstract Optional<T> newValue();

    @Value.Derived
    public boolean added()
    {
        return oldValue().isEmpty() && newValue().isPresent();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @Value.Derived
    public boolean modified()
    {
        if(oldValue().isPresent() && newValue().isPresent()) {
            return !Objects.equals( oldValue(), newValue() );
        }
        return false;
    }

    @Value.Derived
    public boolean removed()
    {
        return oldValue().isPresent() && newValue().isEmpty();
    }

    @Value.Check
    protected void checkNotBothNull()
    {
        Preconditions.checkState( oldValue().isPresent() || newValue().isPresent(),
                                  "Old and new resource can not be empty, is 'spec' missing?" );
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    protected <B> boolean equals( Function<T, B> func )
    {
        if ( !modified() )
        {
            return true;
        }
        B v1 = func.apply( oldValue().get() );
        B v2 = func.apply( newValue().get() );
        return Objects.equals( v1, v2 );
    }

    protected <C, D extends Diff<C>> List<D> mergeLists( List<C> oldList, List<C> newList, BiFunction<Optional<C>, Optional<C>, D> creator )
    {
        Map<Integer, Integer> commonIndexOld = new HashMap<>();
        Map<Integer, Integer> commonIndexNew = new HashMap<>();
        for ( int i = 0; i < oldList.size(); i++ )
        {
            for ( int j = 0; j < newList.size(); j++ )
            {
                if ( oldList.get( i ).equals( newList.get( j ) ) )
                {
                    commonIndexOld.put( i, j );
                    commonIndexNew.put( i, j );
                }
            }
        }

        List<D> res = new LinkedList<>();

        commonIndexOld.forEach(
            ( key, value ) -> res.add( creator.apply( Optional.of( oldList.get( key ) ), Optional.of( newList.get( value ) ) ) ) );

        for ( int i = 0; i < oldList.size(); i++ )
        {
            if ( !commonIndexOld.containsKey( i ) )
            {
                res.add( creator.apply( Optional.of( oldList.get( i ) ), Optional.empty() ) );
            }
        }

        for ( int j = 0; j < newList.size(); j++ )
        {
            if ( !commonIndexNew.containsKey( j ) )
            {
                res.add( creator.apply( Optional.empty(), Optional.of( newList.get( j ) ) ) );
            }
        }

        return res;
    }

    @SuppressWarnings("unused")
    protected <C, D extends Diff<C>> List<D> mergeMaps( Map<String, C> oldMap, Map<String, C> newMap,
                                                        BiFunction<Optional<C>, Optional<C>, D> creator )
    {
        return mergeMaps( oldMap, newMap, ( s, o, n ) -> creator.apply( o, n ) );
    }

    protected <C, D extends Diff<C>> List<D> mergeMaps( Map<String, C> oldMap, Map<String, C> newMap,
                                                        TriFunction<String, Optional<C>, Optional<C>, D> creator )
    {
        Set<String> inBoth = oldMap.keySet().stream().filter( newMap::containsKey ).collect( Collectors.toSet() );
        Set<String> onlyInOld = oldMap.keySet().stream().filter( k -> !inBoth.contains( k ) ).collect( Collectors.toSet() );
        Set<String> onlyInNew = newMap.keySet().stream().filter( k -> !inBoth.contains( k ) ).collect( Collectors.toSet() );

        List<D> res = new LinkedList<>();
        inBoth.forEach( s -> res.add( creator.apply( s, Optional.of( oldMap.get( s ) ), Optional.of( newMap.get( s ) ) ) ) );
        onlyInOld.forEach( s -> res.add( creator.apply( s, Optional.of( oldMap.get( s ) ), Optional.empty() ) ) );
        onlyInNew.forEach( s -> res.add( creator.apply( s, Optional.empty(), Optional.of( newMap.get( s ) ) ) ) );
        return res;
    }
}
