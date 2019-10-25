package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.vhost.ImmutableVHostBuilder;
import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;

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

    public abstract XpDeploymentResourceSpecSharedDisks sharedDisks();

    public abstract List<XpDeploymentResourceSpecNode> nodes();

    protected abstract Map<String, XpDeploymentResourceSpecVHostCertificate> vHostCertificates();

    @Value.Derived
    @JsonIgnore
    public String defaultNamespaceName()
    {
        return String.join( "-", cloud(), project() );
    }

    @Value.Derived
    @JsonIgnore
    public String defaultResourceName()
    {
        return String.join( "-", app(), name() );
    }

    public String defaultResourceNameWithPostFix( String... postfix )
    {
        return String.join( "-", defaultResourceName(), String.join( "-", postfix ) );
    }

    public String defaultResourceNameWithPreFix( String... prefix )
    {
        return String.join( "-", String.join( "-", prefix ), defaultResourceName() );
    }

    @Value.Derived
    @JsonIgnore
    public String deploymentName()
    {
        return String.join( "-", defaultNamespaceName(), defaultResourceName() );
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
            nodes( nodes() ).
            certificates( vHostCertificates() ).
            build().
            execute();
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( app().equals( "xp" ), "field app has to be 'xp' for xp deployments" );
        Preconditions.checkState( nodes().size() > 0, "field 'nodes' has to contain more than 0 nodes" );

        Optional<XpDeploymentResourceSpecNode> singleNode = nodes().stream().filter( n -> n.type() == NodeType.STANDALONE ).findAny();

        Preconditions.checkState( singleNode.isPresent(), "Operator only supports nodes of type " + NodeType.STANDALONE.name() );

        //noinspection ConstantConditions
        Preconditions.checkState( singleNode.isPresent() && nodes().size() == 1,
                                  "you can only have one node there is a node of type " + NodeType.STANDALONE.name() +
                                      " in the node list" );

        singleNode.ifPresent( xpDeploymentResourceSpecNode -> Preconditions.checkState( xpDeploymentResourceSpecNode.replicas() <= 1,
                                                                                        "field replicas on node type " +
                                                                                            NodeType.STANDALONE.name() +
                                                                                            " has to be less than 2" ) );
        for ( Map.Entry<String, List<XpDeploymentResourceSpecNode>> e : nodes().stream().
            collect( Collectors.groupingBy( XpDeploymentResourceSpecNode::alias ) ).entrySet() )
        {
            Preconditions.checkState( e.getValue().size() == 1, "Two nodes have the same alias: " + e.getValue().get( 0 ).alias() );
        }

        for ( VHost vhost : vHosts() )
        {
            for ( VHostPath path : vhost.vHostPaths() )
            {
                Preconditions.checkState( path.nodes().size() == 1,
                                          "Cannot define same vHost path '" + path.path() + "' on same host '" + vhost.host() +
                                              "' on multiple nodes." );
            }
        }
    }
}
