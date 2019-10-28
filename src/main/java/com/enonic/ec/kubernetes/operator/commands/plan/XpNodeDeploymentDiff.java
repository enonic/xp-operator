package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentSpecChangeValidation;

@SuppressWarnings("unchecked")
@Value.Immutable
public abstract class XpNodeDeploymentDiff
{
    private final static Logger log = LoggerFactory.getLogger( XpNodeDeploymentDiff.class );

    abstract Optional<XpDeploymentResource> oldDeployment();

    abstract XpDeploymentResource newDeployment();

    @Value.Derived
    boolean isChangingEnabledDisabled()
    {
        if ( oldDeployment().isEmpty() )
        {
            return false;
        }
        return !oldDeployment().get().getSpec().enabled().equals( newDeployment().getSpec().enabled() );
    }

    @Value.Derived
    boolean isUpdatingXp()
    {
        if ( oldDeployment().isEmpty() )
        {
            return false;
        }
        return !oldDeployment().get().getSpec().xpVersion().equals( newDeployment().getSpec().xpVersion() );
    }

    @Value.Derived
    List<XpDeploymentResourceSpecNode> nodesAdded()
    {
        if ( oldDeployment().isEmpty() )
        {
            return newDeployment().getSpec().nodes();
        }

        List<String> oldAliases =
            oldDeployment().get().getSpec().nodes().stream().map( XpDeploymentResourceSpecNode::alias ).collect( Collectors.toList() );
        return newDeployment().getSpec().nodes().stream().filter( n -> !oldAliases.contains( n.alias() ) ).collect( Collectors.toList() );
    }

    @Value.Derived
    public List<XpNodeDeploymentPlan> deploymentPlans()
    {
        List<XpNodeDeploymentPlan> res = new LinkedList<>();
        nodesAdded().forEach( n -> res.add( ImmutableXpNodeDeploymentPlan.builder().
            nodeTuple( NodeTuple.of( null, n ) ).
            isEnabled( newDeployment().getSpec().enabled() ).
            isNewDeployment( true ).
            isUpdatingXp( false ).
            isChangingEnabledDisabled( false ).
            xpVersion( newDeployment().getSpec().xpVersion() ).
            build() ) );
        nodesChanged().forEach( n -> res.add( ImmutableXpNodeDeploymentPlan.builder().
            nodeTuple( n ).
            isEnabled( newDeployment().getSpec().enabled() ).
            isNewDeployment( false ).
            isUpdatingXp( isUpdatingXp() ).
            isChangingEnabledDisabled( isChangingEnabledDisabled() ).
            xpVersion( newDeployment().getSpec().xpVersion() ).
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

        List<String> newNodeAliases =
            newDeployment().getSpec().nodes().stream().map( XpDeploymentResourceSpecNode::alias ).collect( Collectors.toList() );

        return oldDeployment().get().getSpec().nodes().stream().
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
            Preconditions.checkState( t.oldNode.isPresent(), "oldNode must be present" );
            if ( !t.oldNode.get().equals( t.newNode ) || isChangingEnabledDisabled() || isUpdatingXp() )
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
        for ( XpDeploymentResourceSpecNode oldNode : oldDeployment().get().getSpec().nodes() )
        {
            Optional<XpDeploymentResourceSpecNode> newNode =
                newDeployment().getSpec().nodes().stream().filter( n -> n.alias().equals( oldNode.alias() ) ).findAny();
            newNode.ifPresent( xpDeploymentResourceSpecNode -> res.add( new NodeTuple( oldNode, xpDeploymentResourceSpecNode ) ) );
        }
        return res;
    }

    @Value.Check
    void logInfo()
    {
        if ( oldDeployment().isPresent() )
        {
            XpDeploymentSpecChangeValidation.checkSpec( oldDeployment().get().getSpec(), newDeployment().getSpec() );
        }

        log.debug( "Deployment enabled/disabled: " + isChangingEnabledDisabled() );
        log.debug( "Deployment xp version change: " + isUpdatingXp() );
        log.debug( "Nodes added: " + nodesAdded() );
        log.debug( "Nodes removed: " + nodesRemoved() );
        log.debug( "Nodes changed: " + nodesChanged() );
    }

    @SuppressWarnings("SameParameterValue")
    public static class NodeTuple
    {
        private final Optional<XpDeploymentResourceSpecNode> oldNode;

        private final XpDeploymentResourceSpecNode newNode;

        NodeTuple( final XpDeploymentResourceSpecNode oldNode, final XpDeploymentResourceSpecNode newNode )
        {
            this.oldNode = Optional.ofNullable( oldNode );
            this.newNode = newNode;
        }

        static NodeTuple of( final XpDeploymentResourceSpecNode oldNode, final XpDeploymentResourceSpecNode newNode )
        {
            return new NodeTuple( oldNode, newNode );
        }

        Optional<XpDeploymentResourceSpecNode> getOldNode()
        {
            return oldNode;
        }

        XpDeploymentResourceSpecNode getNewNode()
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
