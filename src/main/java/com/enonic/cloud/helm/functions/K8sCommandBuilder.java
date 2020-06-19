package com.enonic.cloud.helm.functions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.builders.GenericBuilderAction;
import com.enonic.cloud.kubernetes.commands.builders.GenericBuilderParamsImpl;

import static com.enonic.cloud.common.Configuration.cfgStr;


@Singleton
public class K8sCommandBuilder
    implements Function<K8sCommandBuilderParams, List<Optional<K8sCommand>>>
{
    private static final Function<HasMetadata, String> merge = ( r ) -> r.getKind() + "#" + r.getMetadata().getName();

    @Override
    public List<Optional<K8sCommand>> apply( final K8sCommandBuilderParams params )
    {
        List<Optional<K8sCommand>> res = new LinkedList<>();

        Map<String, HasMetadata> oldResources = params.oldResources().stream().collect( Collectors.toMap( merge, r -> r ) );
        Map<String, HasMetadata> newResources = params.newResources().stream().collect( Collectors.toMap( merge, r -> r ) );

        for ( Map.Entry<String, HasMetadata> r : oldResources.entrySet() )
        {
            if ( !newResources.containsKey( r.getKey() ) )
            {
                GenericBuilderParamsImpl.Builder paramBuilder = GenericBuilderParamsImpl.
                    builder().
                    resource( r.getValue() ).
                    action( GenericBuilderAction.DELETE );

                res.add( params.k8sCommandMapper().getCommand( paramBuilder.build() ) );
            }
        }

        for ( Map.Entry<String, HasMetadata> r : newResources.entrySet() )
        {
            GenericBuilderParamsImpl.Builder paramBuilder = GenericBuilderParamsImpl.
                builder().
                resource( r.getValue() ).
                action( GenericBuilderAction.APPLY );

            applyOptions( r.getValue().getMetadata().getAnnotations(), paramBuilder );

            res.add( params.k8sCommandMapper().getCommand( paramBuilder.build() ) );
        }

        return res;
    }

    private void applyOptions( final Map<String, String> annotations, final GenericBuilderParamsImpl.Builder paramBuilder )
    {
        boolean alwaysOverwrite = false;
        boolean neverModify = false;
        if ( annotations != null )
        {
            alwaysOverwrite =
                Objects.equals( annotations.get( cfgStr( "operator.helm.charts.Values.annotationKeys.alwaysOverwrite" ) ), "true" );
            neverModify =
                Objects.equals( annotations.get( cfgStr( "operator.helm.charts.Values.annotationKeys.neverOverwrite" ) ), "true" );
        }
        paramBuilder.alwaysOverwrite( alwaysOverwrite );
        paramBuilder.neverModify( neverModify );
    }

}
