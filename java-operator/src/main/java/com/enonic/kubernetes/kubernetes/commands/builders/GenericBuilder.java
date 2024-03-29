package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.kubernetes.commands.ImmutableK8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandAction;

import static com.enonic.kubernetes.common.Configuration.cfgStr;

@SuppressWarnings("WeakerAccess")
public abstract class GenericBuilder<C, R extends HasMetadata>
    implements Function<GenericBuilderParams<R>, Optional<K8sCommand>>
{
    protected abstract C client();

    @Override
    public Optional<K8sCommand> apply( final GenericBuilderParams<R> params )
    {
        // If always override, do not bother to check old resource
//        if ( params.action() == GenericBuilderAction.APPLY && params.alwaysOverwrite() )
//        {
//            return Optional.of( ImmutableK8sCommand.builder().
//                action( K8sCommandAction.OVERWRITE ).
//                resource( params.resource() ).
//                wrappedRunnable( createOrReplaceCommand( params.resource().getMetadata().getNamespace(), params.resource() ) ).
//                build() );
//        }

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
                return Optional.of( ImmutableK8sCommand.builder().
                    action( K8sCommandAction.DELETE ).
                    resource( oldResource.get() ).
                    wrappedRunnable( deleteCommand( oldResource.get().getMetadata().getNamespace(), oldResource.get() ) ).
                    build() );
            }
        }

        if ( oldResource.isEmpty() )
        {
            return Optional.of( ImmutableK8sCommand.builder().
                action( K8sCommandAction.CREATE ).
                resource( params.resource() ).
                wrappedRunnable( createOrReplaceCommand( params.resource().getMetadata().getNamespace(), params.resource() ) ).
                build() );
        }

        // If both old and new have never overwrite
        if ( neverOverwrite( oldResource.get() ) && neverOverwrite( params.resource() ) )
        {
            return Optional.empty();
        }

        if ( !equals( oldResource.get(), params.resource() ) )

        {
            return Optional.of( ImmutableK8sCommand.builder().
                action( K8sCommandAction.UPDATE ).
                resource( params.resource() ).
                wrappedRunnable( updateCommand( params.resource().getMetadata().getNamespace(), params.resource() ) ).
                build() );
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
//        if ( !equalSpec )
//        {
//            ObjectMapper mapper = new ObjectMapper();
//            try
//            {
//                System.out.println( mapper.writeValueAsString( o ) );
//                System.out.println( mapper.writeValueAsString( n ) );
//            }
//            catch ( JsonProcessingException e )
//            {
//                e.printStackTrace();
//            }
//        }

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

    protected abstract Optional<R> getOldResource( final String namespace, final String name );

    protected abstract Runnable createOrReplaceCommand( final String namespace, final R r );

    protected abstract Runnable updateCommand( final String namespace, final R r );

    protected abstract Runnable deleteCommand( final String namespace, final R r );

}
