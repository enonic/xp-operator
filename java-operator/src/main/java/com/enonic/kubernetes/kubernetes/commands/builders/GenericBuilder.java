package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.enonic.kubernetes.common.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandAction;

import static com.enonic.kubernetes.common.Configuration.cfgStr;

@SuppressWarnings("WeakerAccess")
public abstract class GenericBuilder<C, R extends HasMetadata>
    implements Function<GenericBuilderParams<R>, Optional<K8sCommand>>
{
    private final C client;

    protected GenericBuilder( final C client )
    {
        this.client = client;
    }

    protected C client()
    {
        return client;
    }

    @Override
    public Optional<K8sCommand> apply( final GenericBuilderParams<R> params )
    {
        Optional<R> oldResource =
            getOldResource( params.resource().getMetadata().getNamespace(), params.resource().getMetadata().getName() );

        if ( params.action() == GenericBuilderAction.DELETE )
        {
            if ( oldResource.isEmpty() )
            {
                return Optional.empty();
            }
            else
            {
                return Optional.of( new K8sCommand( K8sCommandAction.DELETE, oldResource.get(),
                                                deleteCommand( oldResource.get().getMetadata().getNamespace(), oldResource.get() ) ) );
            }
        }

        if ( oldResource.isEmpty() )
        {
            return Optional.of( new K8sCommand( K8sCommandAction.CREATE, params.resource(),
                                                createOrReplaceCommand( params.resource().getMetadata().getNamespace(), params.resource() ) ) );
        }

        // If both old and new have never overwrite
        if ( neverOverwrite( oldResource.get() ) && neverOverwrite( params.resource() ) )
        {
            return Optional.empty();
        }

        if ( !equals( oldResource.get(), params.resource() ) )

        {
            return Optional.of( new K8sCommand( K8sCommandAction.UPDATE, params.resource(),
                                                updateCommand( params.resource().getMetadata().getNamespace(), params.resource() ) ) );
        }

        return Optional.empty();
    }

    private boolean neverOverwrite( final R r )
    {
        return r.getMetadata().getAnnotations() != null && Objects.equals( r.
            getMetadata().
            getAnnotations().
            get( cfgStr( "operator.charts.values.annotationKeys.neverOverwrite" ) ), "true" );
    }

    protected boolean equals( final R o, final R n )
    {
        Preconditions.checkState( Objects.equals( o.getMetadata().getName(), n.getMetadata().getName() ), "Names do not match" );
        Preconditions.checkState( Objects.equals( o.getMetadata().getNamespace(), n.getMetadata().getNamespace() ), "Namespaces do not match" );

        if ( !equalLabels( getOrDefault( o.getMetadata().getLabels() ), getOrDefault( n.getMetadata().getLabels() ) ) )
        {
            return false;
        }

        if ( !equalAnnotations( getOrDefault( o.getMetadata().getAnnotations() ), getOrDefault( n.getMetadata().getAnnotations() ) ) )
        {
            return false;
        }

        boolean equalSpec = equalsSpec( o, n );

        return equalSpec;
    }

    protected boolean equalLabels( Map<String, String> o, Map<String, String> n )
    {
        return Objects.equals( o, n );
    }

    protected boolean equalAnnotations( final Map<String, String> o, final Map<String, String> n )
    {
        return Objects.equals( o, n );
    }

    protected boolean equalsSpec( final R o, final R n )
    {
        return Objects.equals( o, n );
    }

    private <T> T getOrDefault( T value, Supplier<T> sup )
    {
        if ( value == null )
        {
            return sup.get();
        }
        return value;
    }

    private Map<String, String> getOrDefault( Map<String, String> m )
    {
        return getOrDefault( m, Collections::emptyMap );
    }

    protected abstract Optional<R> getOldResource( String namespace, String name );

    protected abstract Runnable createOrReplaceCommand( String namespace, R r );

    protected abstract Runnable updateCommand( String namespace, R r );

    protected abstract Runnable deleteCommand( String namespace, R r );

}
