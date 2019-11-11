package com.enonic.ec.kubernetes.crd.vhost.spec;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.common.Configuration;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
public abstract class Spec
    extends Configuration
{
    public abstract String host();

    public abstract Optional<SpecCertificate> certificate();

    public abstract List<SpecMapping> mappings();

//    public boolean doesModify( ConfigMap configMap ) // TODO: Change to alias
//    {
//        String alias = configMap.getMetadata().
//            getLabels().
//            get( cfgStrFmt( "operator.deployment.xp.pod.label.aliasPrefix", "main" ) ); // TODO: FIX
//
//        if ( alias == null )
//        {
//            return false;
//        }
//
//        return mappings().stream().filter( m -> m.nodeAlias().equals( alias ) ).findAny().isPresent();
//    }

    public Optional<Spec> relevantSpec( String nodeAlias )
    {
        List<SpecMapping> relevantMappings =
            mappings().stream().filter( m -> m.nodeAlias().equals( nodeAlias ) ).collect( Collectors.toList() );
        if ( relevantMappings.size() < 1 )
        {
            return Optional.empty();
        }
        return Optional.of( ImmutableSpec.builder().from( this ).mappings( relevantMappings ).build() );
    }
}
