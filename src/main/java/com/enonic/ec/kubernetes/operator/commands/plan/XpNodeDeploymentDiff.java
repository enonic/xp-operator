package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class XpNodeDeploymentDiff
{
    private final static Logger log = LoggerFactory.getLogger( XpNodeDeploymentDiff.class );

    abstract Optional<XpDeploymentResource> oldDeployment();

    abstract XpDeploymentResource newDeployment();

    @Value.Derived
    boolean enabledDisabledChanged()
    {
        if ( oldDeployment().isEmpty() )
        {
            return false;
        }
        return !oldDeployment().get().spec().enabled().equals( newDeployment().spec().enabled() );
    }

    @Value.Derived
    boolean updateXp()
    {
        if ( oldDeployment().isEmpty() )
        {
            return false;
        }
        return !oldDeployment().get().spec().xpVersion().equals( newDeployment().spec().xpVersion() );
    }

    @Value.Derived
    List<XpDeploymentResourceSpecNode> nodesAdded()
    {
        if ( oldDeployment().isEmpty() )
        {
            return newDeployment().spec().nodes();
        }

        List<String> oldAliases = oldDeployment().get().spec().nodes().stream().map( n -> n.alias() ).collect( Collectors.toList() );
        return newDeployment().spec().nodes().stream().filter( n -> !oldAliases.contains( n.alias() ) ).collect( Collectors.toList() );
    }

    @Value.Derived
    public List<XpNodeDeploymentPlan> deploymentPlans()
    {
        List<XpNodeDeploymentPlan> res = new LinkedList<>();
        nodesAdded().forEach( n -> res.add( ImmutableXpNodeDeploymentPlan.builder().
            nodeTuple( NodeTuple.of( null, n ) ).
            newDeployment( true ).
            build() ) );
        nodesChanged().forEach( n -> res.add( ImmutableXpNodeDeploymentPlan.builder().
            nodeTuple( n ).
            newDeployment( false ).
            enabled( newDeployment().spec().enabled() ).
            enabledDisabledChanged( enabledDisabledChanged() ).
            updateXp( updateXp() ).
            build() ) );
        return res;
    }

    @Value.Derived
    public List<XpDeploymentResourceSpecNode> nodesRemoved()
    {
        if ( oldDeployment().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List<String> newNodeAliases = newDeployment().spec().nodes().stream().map( n -> n.alias() ).collect( Collectors.toList() );

        return oldDeployment().get().spec().nodes().stream().
            filter( n -> !newNodeAliases.contains( n.alias() ) ).
            collect( Collectors.toList() );
    }

    @Value.Derived
    List<NodeTuple> nodesChanged()
    {
        if ( oldDeployment().isEmpty() )
        {
            // No old deployment
            return Collections.EMPTY_LIST;
        }

        List<NodeTuple> changes = new LinkedList<>();
        for ( NodeTuple t : nodeTuples() )
        {
            if ( !t.oldNode.equals( t.newNode ) || enabledDisabledChanged() )
            {
                changes.add( t );
            }
        }
        return changes;
    }

    @Value.Derived
    List<NodeTuple> nodeTuples()
    {
        if ( oldDeployment().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List<NodeTuple> res = new LinkedList<>();
        for ( XpDeploymentResourceSpecNode oldNode : oldDeployment().get().spec().nodes() )
        {
            Optional<XpDeploymentResourceSpecNode> newNode =
                newDeployment().spec().nodes().stream().filter( n -> n.alias().equals( oldNode.alias() ) ).findAny();
            if ( newNode.isPresent() )
            {
                res.add( new NodeTuple( oldNode, newNode.get() ) );
            }
        }
        return res;
    }

    @Value.Check
    void logInfo()
    {
        log.debug( "Deployment enabled/disabled: " + enabledDisabledChanged() );
        log.debug( "Deployment xp version change: " + updateXp() );
        log.debug( "Nodes added: " + nodesAdded() );
        log.debug( "Nodes removed: " + nodesRemoved() );
        log.debug( "Nodes changed: " + nodesChanged() );
    }

    public static class NodeTuple
    {
        private final Optional<XpDeploymentResourceSpecNode> oldNode;

        private final XpDeploymentResourceSpecNode newNode;

        protected NodeTuple( final XpDeploymentResourceSpecNode oldNode, final XpDeploymentResourceSpecNode newNode )
        {
            this.oldNode = Optional.ofNullable( oldNode );
            this.newNode = newNode;
        }

        public static NodeTuple of( final XpDeploymentResourceSpecNode oldNode, final XpDeploymentResourceSpecNode newNode )
        {
            return new NodeTuple( oldNode, newNode );
        }

        protected Optional<XpDeploymentResourceSpecNode> getOldNode()
        {
            return oldNode;
        }

        protected XpDeploymentResourceSpecNode getNewNode()
        {
            return newNode;
        }

        @Override
        public String toString()
        {
            return newNode.alias();
        }
    }
}
