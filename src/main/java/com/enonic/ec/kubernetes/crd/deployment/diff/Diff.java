package com.enonic.ec.kubernetes.crd.deployment.diff;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    public boolean isNew()
    {
        return oldValue().isEmpty() && newValue().isPresent();
    }

    @Value.Derived
    public boolean shouldAddOrModify()
    {
        return ( oldValue().isEmpty() && newValue().isPresent() ) || shouldModify();
    }

    @Value.Derived
    public boolean shouldModify()
    {
        return oldValue().isPresent() && newValue().isPresent() && !oldValue().equals( newValue() );
    }

    @Value.Derived
    public boolean shouldRemove()
    {
        return oldValue().isPresent() && newValue().isEmpty();
    }

    @Value.Derived
    public boolean shouldAddOrModifyOrRemove()
    {
        return shouldAddOrModify() || shouldRemove();
    }

    @Value.Check
    protected void checkNotBothNull()
    {
        Preconditions.checkState( oldValue().isPresent() || newValue().isPresent(), "both old and new value can't be empty" );
    }

    //region helper functions

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    <B> boolean equals( Function<T, B> func )
    {
        if ( !shouldModify() )
        {
            return false;
        }
        B v1 = func.apply( oldValue().get() );
        B v2 = func.apply( newValue().get() );
        return v1.equals( v2 );
    }

    <C, D extends Diff<C>> List<D> mergeMaps( Map<String, C> oldMap, Map<String, C> newMap,
                                              BiFunction<Optional<C>, Optional<C>, D> creator )
    {
        Set<String> inBoth = oldMap.keySet().stream().filter( newMap::containsKey ).collect( Collectors.toSet() );
        Set<String> onlyInOld = oldMap.keySet().stream().filter( k -> !inBoth.contains( k ) ).collect( Collectors.toSet() );
        Set<String> onlyInNew = newMap.keySet().stream().filter( k -> !inBoth.contains( k ) ).collect( Collectors.toSet() );

        List<D> res = new LinkedList<>();
        inBoth.forEach( s -> res.add( creator.apply( Optional.of( oldMap.get( s ) ), Optional.of( newMap.get( s ) ) ) ) );
        onlyInOld.forEach( s -> res.add( creator.apply( Optional.of( oldMap.get( s ) ), Optional.empty() ) ) );
        onlyInNew.forEach( s -> res.add( creator.apply( Optional.empty(), Optional.of( newMap.get( s ) ) ) ) );
        return res;
    }

    //endregion
}
