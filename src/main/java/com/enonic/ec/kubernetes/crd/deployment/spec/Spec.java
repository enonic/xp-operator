package com.enonic.ec.kubernetes.crd.deployment.spec;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.ec.kubernetes.crd.deployment.vhost.ImmutableVHostBuilder;
import com.enonic.ec.kubernetes.crd.deployment.vhost.VHost;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
public abstract class Spec
{
    public abstract String xpVersion();

    public abstract String cloud();

    public abstract String project();

    @Value.Default
    public String app()
    {
        return "xp";
    }

    public abstract String name();

    public abstract Boolean enabled();

    public abstract Quantity sharedDisk();

    public abstract List<SpecNode> nodes();

    public abstract Map<String, SpecVHostCertificate> vHostCertificates();

    //region class consistency check

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( app().equals( "xp" ), "field 'app' has to be 'xp' for xp deployments" );
        Preconditions.checkState( nodes().size() > 0, "field 'nodes' has to contain more than 0 nodes" );

        Preconditions.checkState( nodes().stream().filter( SpecNode::isMasterNode ).count() == 1,
                                  "1 and only 1 node has be of type master" );
        Preconditions.checkState( nodes().stream().filter( SpecNode::isDataNode ).count() == 1, "1 and only node has be of type data" );

        Set<String> aliases = nodes().stream().map( SpecNode::alias ).collect( Collectors.toSet() );
        Preconditions.checkState( aliases.size() == nodes().size(), "nodes cannot have the same alias" );
    }

    //endregion

    //region derived functions

    @Value.Default
    @JsonIgnore
    public boolean isClustered()
    {
        return nodes().stream().mapToInt( SpecNode::replicas ).sum() > 1;
    }

    @Value.Derived
    @JsonIgnore
    public String deploymentName()
    {
        return String.join( "-", cloud(), project(), app(), name() );
    }

    @Value.Derived
    @JsonIgnore
    public Map<String, String> defaultLabels()
    {
        return Map.of( "cloud", cloud(), "project", project(), "app", app(), "name", name() );
    }

    @Value.Derived
    @JsonIgnore
    public List<VHost> vHosts()
    {
        return ImmutableVHostBuilder.builder().
            spec( this ).
            build().
            vHosts();
    }

    //endregion
}