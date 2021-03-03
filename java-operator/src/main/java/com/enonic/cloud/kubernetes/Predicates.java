package com.enonic.cloud.kubernetes;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;

import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class Predicates
{
    public static <T> void onCondition( T o, Consumer<T> c, Predicate<? super T>... predicates )
    {
        for ( Predicate p : predicates )
        {
            if ( !p.test( o ) )
            {
                return;
            }
        }
        c.accept( o );
    }

    public static Predicate<HasMetadata> inNamespace( String namespace )
    {
        return ( r ) -> Objects.equals( r.getMetadata().getNamespace(), namespace );
    }

    public static Predicate<HasMetadata> inSameNamespace( HasMetadata m )
    {
        return inNamespace( m.getMetadata().getNamespace() );
    }

    public static Predicate<Namespace> matchNamespace( HasMetadata m )
    {
        return ( r ) -> Objects.equals( r.getMetadata().getName(), m.getMetadata().getNamespace() );
    }

    public static Predicate<HasMetadata> withName( String name )
    {
        return ( r ) -> Objects.equals( r.getMetadata().getName(), name );
    }

    public static Predicate<HasMetadata> hasLabel( String key )
    {
        return ( r ) -> r.getMetadata().getLabels() != null && r.getMetadata().getLabels().containsKey( key );
    }

    public static Predicate<HasMetadata> matchLabel( String key, String value )
    {
        return ( r ) -> r.getMetadata().getLabels() != null && Objects.equals( r.getMetadata().getLabels().get( key ), value );
    }

    public static Predicate<HasMetadata> hasAnnotation( String key )
    {
        return ( r ) -> r.getMetadata().getAnnotations() != null && r.getMetadata().getAnnotations().containsKey( key );
    }

    public static Predicate<HasMetadata> matchAnnotation( String key, String value )
    {
        return ( r ) -> r.getMetadata().getAnnotations() != null && Objects.equals( r.getMetadata().getAnnotations().get( key ), value );
    }

    public static Predicate<HasMetadata> matchAnnotationPrefix( String key )
    {
        return ( r ) -> {
            if ( r.getMetadata().getAnnotations() == null )
            {
                return false;
            }
            for ( String k : r.getMetadata().getAnnotations().keySet() )
            {
                if ( k.startsWith( key ) )
                {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<HasMetadata> hasFinalizer( String key )
    {
        return ( r ) -> r.getMetadata().getFinalizers() != null && r.getMetadata().getFinalizers().contains( key );
    }

    public static Predicate<HasMetadata> createdBefore( Instant instant )
    {
        return ( r ) -> Instant.parse( r.getMetadata().getCreationTimestamp() ).isBefore( instant );
    }

    public static Predicate<HasMetadata> createdAfter( Instant instant )
    {
        return ( r ) -> Instant.parse( r.getMetadata().getCreationTimestamp() ).isAfter( instant );
    }

    public static Predicate<HasMetadata> olderThan( long seconds )
    {
        return createdBefore( Instant.now().minusSeconds( seconds ) );
    }

    public static Predicate<HasMetadata> youngerThan( long seconds )
    {
        return createdAfter( Instant.now().minusSeconds( seconds ) );
    }

    public static Predicate<HasMetadata> isEnonicManaged()
    {
        return ( r ) -> ( r.getMetadata().getLabels() != null &&
            Objects.equals( r.getMetadata().getLabels().get( cfgStr( "operator.charts.values.labelKeys.managed" ) ), "true" ) );
    }

    public static Predicate<HasMetadata> isDeleted()
    {
        return ( r ) -> r.getMetadata().getDeletionTimestamp() != null;
    }

    public static Predicate<HasMetadata> isBeingBackupRestored()
    {
        return hasLabel( cfgStr( "operator.charts.values.labelKeys.veleroBackupName" ) ).
            and( hasLabel( cfgStr( "operator.charts.values.labelKeys.veleroBackupRestore" ) ) ).
            and( youngerThan( 60 ) );
    }

    public static <T, R> Predicate<T> fieldEquals( T o, Function<T, R> f )
    {
        return ( r ) -> Objects.equals( f.apply( o ), f.apply( r ) );
    }

    public static <T, R> Predicate<T> fieldsEquals( T o, Function<T, R>... functions )
    {
        Predicate<T> p = ( a ) -> true;
        for ( Function<T, R> f : functions )
        {
            p = p.and( fieldEquals( o, f ) );
        }
        return p;
    }

    public static Predicate<ConfigMap> dataEquals( ConfigMap c )
    {
        return fieldEquals( c, ConfigMap::getData ).and( fieldEquals( c, ConfigMap::getBinaryData ) );
    }

    public static Predicate<Xp7Config> inNodeGroup( String... nodegroup )
    {
        return ( r ) -> Arrays.asList( nodegroup ).contains( r.getSpec().getNodeGroup() );
    }

    public static Predicate<Xp7Config> inNodeGroupAllOr( String nodeGroup )
    {
        return inNodeGroup( cfgStr( "operator.charts.values.allNodesKey" ), nodeGroup );
    }

    public static Predicate<HasMetadata> isPartOfDeployment( String name )
    {
        return matchLabel( cfgStr( "operator.charts.values.labelKeys.deployment" ), name );
    }

    public static Predicate<HasMetadata> isPartOfDeployment( Xp7Deployment deployment )
    {
        return isPartOfDeployment( deployment.getMetadata().getName() );
    }
}
