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

import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;

@SuppressWarnings("unchecked")
@Value.Immutable
public abstract class XpVHostDeploymentDiff
{
    private final static Logger log = LoggerFactory.getLogger( XpVHostDeploymentDiff.class );

    public abstract Optional<XpDeploymentResource> oldDeployment();

    public abstract XpDeploymentResource newDeployment();

    @Value.Derived
    protected List<VHost> vHostsAdded()
    {
        if ( oldDeployment().isEmpty() )
        {
            return newDeployment().getSpec().vHosts();
        }
        List<String> oldHosts = oldDeployment().get().getSpec().vHosts().stream().map( VHost::host ).collect( Collectors.toList() );
        return newDeployment().getSpec().vHosts().stream().filter( h -> !oldHosts.contains( h.host() ) ).collect( Collectors.toList() );
    }

    @Value.Derived
    public List<XpVHostDeploymentPlan> deploymentPlans()
    {
        List<XpVHostDeploymentPlan> res = new LinkedList<>();
        vHostsAdded().forEach( n -> res.add( ImmutableXpVHostDeploymentPlan.builder().
            vHostTuple( VHostTuple.of( null, n ) ).
            build() ) );
        vHostsChanged().forEach( n -> res.add( ImmutableXpVHostDeploymentPlan.builder().
            vHostTuple( n ).
            build() ) );
        return res;
    }

    @Value.Derived
    public List<VHost> vHostsRemoved()
    {
        if ( oldDeployment().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }
        List<String> newVHosts = newDeployment().getSpec().vHosts().stream().map( VHost::host ).collect( Collectors.toList() );
        return oldDeployment().get().getSpec().vHosts().stream().
            filter( h -> !newVHosts.contains( h.host() ) ).
            collect( Collectors.toList() );
    }

    @Value.Derived
    protected List<VHostTuple> vHostsChanged()
    {
        if ( oldDeployment().isEmpty() )
        {
            // No old deployment
            return Collections.EMPTY_LIST;
        }

        List<VHostTuple> changes = new LinkedList<>();
        for ( VHostTuple t : vHostTuples() )
        {
            Preconditions.checkState( t.oldVHost.isPresent(), "oldVHost must be present" );
            if ( !t.oldVHost.get().equals( t.newVHost ) )
            {
                changes.add( t );
            }
        }
        return changes;
    }

    @Value.Derived
    protected List<VHostTuple> vHostTuples()
    {
        if ( oldDeployment().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List<VHostTuple> res = new LinkedList<>();
        for ( VHost oldVHost : oldDeployment().get().getSpec().vHosts() )
        {
            Optional<VHost> newVHost =
                newDeployment().getSpec().vHosts().stream().filter( h -> h.host().equals( oldVHost.host() ) ).findAny();
            newVHost.ifPresent( vHost -> res.add( new VHostTuple( oldVHost, vHost ) ) );
        }
        return res;
    }

    @Value.Check
    protected void logInfo()
    {
        log.debug( "vHosts added: " + vHostsAdded() );
        log.debug( "vHosts removed: " + vHostsRemoved() );
        log.debug( "vHosts changed: " + vHostsChanged() );
    }

    @SuppressWarnings("ALL")
    public static class VHostTuple
    {
        private final Optional<VHost> oldVHost;

        private final VHost newVHost;

        VHostTuple( final VHost oldVHost, final VHost newVHost )
        {
            this.oldVHost = Optional.ofNullable( oldVHost );
            this.newVHost = newVHost;
        }

        static VHostTuple of( final VHost oldVHost, final VHost newVHost )
        {
            return new VHostTuple( oldVHost, newVHost );
        }

        public Optional<VHost> getOldVHost()
        {
            return oldVHost;
        }

        public VHost getNewVHost()
        {
            return newVHost;
        }

        @Override
        public String toString()
        {
            return newVHost.host();
        }
    }
}
