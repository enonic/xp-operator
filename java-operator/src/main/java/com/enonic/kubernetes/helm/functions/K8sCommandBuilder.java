package com.enonic.kubernetes.helm.functions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.builders.GenericBuilderAction;
import com.enonic.kubernetes.kubernetes.commands.builders.GenericBuilderParamsImpl;


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

            res.add( params.k8sCommandMapper().getCommand( paramBuilder.build() ) );
        }

        return res;
    }
}
