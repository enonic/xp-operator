package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpec.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpec
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

    @Value.Default
    public Boolean enabled()
    {
        return true;
    }

    public abstract List<XpDeploymentResourceSpecNode> nodes();

    @Nullable
    public abstract Map<String, XpDeploymentResourceSpecVhostCertificate> vHostCertificates();

    @Value.Derived
    @JsonIgnore
    public String fullProjectName()
    {
        return String.join( "-", cloud(), project() );
    }

    @Value.Derived
    @JsonIgnore
    public String fullAppName()
    {
        return String.join( "-", app(), name() );
    }

    @Value.Derived
    @JsonIgnore
    public String fullName()
    {
        return String.join( "-", fullProjectName(), fullAppName() );
    }

    @Value.Derived
    @JsonIgnore
    public Map<String, String> defaultLabels()
    {
        return Map.of( "cloud", cloud(), "project", project(), "app", app(), "name", name() );
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( app().equals( "xp" ), "field app has to be 'xp' for xp deployments" );

        Optional<XpDeploymentResourceSpecNode> singleNode = nodes().stream().filter( n -> n.type() == NodeType.STANDALONE ).findAny();

        Preconditions.checkState( singleNode.isPresent() || nodes().size() == 1,
                                  "you can only have one node if a node of type " + NodeType.STANDALONE.name() + "is in the node list" );
        Preconditions.checkState( singleNode.isPresent(), "Operator only supports nodes of type " + NodeType.STANDALONE.name() );

        nodes().forEach( node -> {
            Preconditions.checkState( node.resources().disks().containsKey( "repo" ), "field resources.disks.repo on node has to be set" );
            Preconditions.checkState( node.resources().disks().containsKey( "snapshots" ),
                                      "field resources.disks.repo on node has to be set" );
        } );

        singleNode.ifPresent( xpDeploymentResourceSpecNode -> Preconditions.checkState( xpDeploymentResourceSpecNode.replicas().equals( 1 ),
                                                                                        "field replicas on node type " +
                                                                                            NodeType.STANDALONE.name() + " has to be 1" ) );
        for ( Map.Entry<String, List<XpDeploymentResourceSpecNode>> e : nodes().stream().
            collect( Collectors.groupingBy( XpDeploymentResourceSpecNode::alias ) ).entrySet() )
        {
            Preconditions.checkState( e.getValue().size() == 1, "two nodes have the same alias: " + e.getValue().get( 0 ).alias() );
        }
    }
}
