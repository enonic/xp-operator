package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

@Value.Immutable
public abstract class XpNodeDeploymentDiff
{
    private final static Logger log = LoggerFactory.getLogger( XpNodeDeploymentDiff.class );

    @Nullable
    public abstract XpDeploymentResource oldDeployment();

    public abstract XpDeploymentResource newDeployment();

    @Value.Derived
    public boolean enabledDisabled()
    {
        if ( oldDeployment() == null )
        {
            return false;
        }
        return !oldDeployment().getSpec().enabled().equals( newDeployment().getSpec().enabled() );
    }

    @Value.Derived
    protected boolean updateXp()
    {
        if ( oldDeployment() == null )
        {
            return false;
        }
        return !oldDeployment().getSpec().xpVersion().equals( newDeployment().getSpec().xpVersion() );
    }

    @Value.Derived
    protected List<XpDeploymentResourceSpecNode> nodesAdded()
    {
        if ( oldDeployment() == null )
        {
            return newDeployment().getSpec().nodes();
        }

        List<String> oldAliases = oldDeployment().getSpec().nodes().stream().map( n -> n.alias() ).collect( Collectors.toList() );
        return newDeployment().getSpec().nodes().stream().filter( n -> !oldAliases.contains( n.alias() ) ).collect( Collectors.toList() );
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
            enabledDisabled( enabledDisabled() ).
            updateXp( updateXp() ).
            build() ) );
        return res;
    }

    @Value.Derived
    public List<XpDeploymentResourceSpecNode> nodesRemoved()
    {
        if ( oldDeployment() == null )
        {
            return Collections.EMPTY_LIST;
        }

        List<String> newNodeAliases = newDeployment().getSpec().nodes().stream().map( n -> n.alias() ).collect( Collectors.toList() );

        return oldDeployment().getSpec().nodes().stream().
            filter( n -> !newNodeAliases.contains( n.alias() ) ).
            collect( Collectors.toList() );
    }

    @Value.Derived
    protected List<NodeTuple> nodesChanged()
    {
        if ( oldDeployment() == null )
        {
            // No old deployment
            return Collections.EMPTY_LIST;
        }

        List<NodeTuple> changes = new LinkedList<>();
        for ( NodeTuple t : nodeTuples() )
        {
            if ( !t.oldNode.equals( t.newNode ) )
            {
                changes.add( t );
            }
        }
        return changes;
    }

    @Value.Derived
    protected List<NodeTuple> nodeTuples()
    {
        if ( oldDeployment() == null )
        {
            return Collections.EMPTY_LIST;
        }

        List<NodeTuple> res = new LinkedList<>();
        for ( XpDeploymentResourceSpecNode oldNode : oldDeployment().getSpec().nodes() )
        {
            Optional<XpDeploymentResourceSpecNode> newNode =
                newDeployment().getSpec().nodes().stream().filter( n -> n.alias().equals( oldNode.alias() ) ).findAny();
            if ( newNode.isPresent() )
            {
                res.add( new NodeTuple( oldNode, newNode.get() ) );
            }
        }
        return res;
    }

    @Value.Check
    protected void logInfo()
    {
        log.debug( "Deployment enabled/disabled: " + enabledDisabled() );
        log.debug( "Deployment xp version change: " + updateXp() );
        log.debug( "Nodes added: " + nodesAdded() );
        log.debug( "Nodes removed: " + nodesRemoved() );
        log.debug( "Nodes changed: " + nodesChanged() );
    }

    public static class NodeTuple
    {
        private final XpDeploymentResourceSpecNode oldNode;

        private final XpDeploymentResourceSpecNode newNode;

        protected NodeTuple( final XpDeploymentResourceSpecNode oldNode, final XpDeploymentResourceSpecNode newNode )
        {
            this.oldNode = oldNode;
            this.newNode = newNode;
        }

        public static NodeTuple of( final XpDeploymentResourceSpecNode oldNode, final XpDeploymentResourceSpecNode newNode )
        {
            return new NodeTuple( oldNode, newNode );
        }

        protected XpDeploymentResourceSpecNode getOldNode()
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
