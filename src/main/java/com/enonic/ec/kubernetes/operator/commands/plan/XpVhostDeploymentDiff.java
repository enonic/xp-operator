package com.enonic.ec.kubernetes.operator.commands.plan;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.ec.kubernetes.deployment.vhost.Vhost;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;

@Value.Immutable
public abstract class XpVhostDeploymentDiff
{
    private final static Logger log = LoggerFactory.getLogger( XpVhostDeploymentDiff.class );

    public abstract Optional<XpDeploymentResource> oldDeployment();

    public abstract XpDeploymentResource newDeployment();

    @Value.Derived
    protected List<Vhost> vHostsAdded()
    {
        if ( oldDeployment().isEmpty() )
        {
            return newDeployment().spec().vHosts();
        }
        List<String> oldHosts = oldDeployment().get().spec().vHosts().stream().map( h -> h.host() ).collect( Collectors.toList() );
        return newDeployment().spec().vHosts().stream().filter( h -> !oldHosts.contains( h.host() ) ).collect( Collectors.toList() );
    }

    @Value.Derived
    public List<XpVhostDeploymentPlan> deploymentPlans()
    {
        List<XpVhostDeploymentPlan> res = new LinkedList<>();
        vHostsAdded().forEach( n -> res.add( ImmutableXpVhostDeploymentPlan.builder().
            vHostTuple( VhostTuple.of( null, n ) ).
            build() ) );
        vHostsChanged().forEach( n -> res.add( ImmutableXpVhostDeploymentPlan.builder().
            vHostTuple( n ).
            build() ) );
        return res;
    }

    @Value.Derived
    public List<Vhost> vHostsRemoved()
    {
        if ( oldDeployment().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }
        List<String> newVHosts = newDeployment().spec().vHosts().stream().map( h -> h.host() ).collect( Collectors.toList() );
        return oldDeployment().get().spec().vHosts().stream().
            filter( h -> !newVHosts.contains( h.host() ) ).
            collect( Collectors.toList() );
    }

    @Value.Derived
    protected List<VhostTuple> vHostsChanged()
    {
        if ( oldDeployment().isEmpty() )
        {
            // No old deployment
            return Collections.EMPTY_LIST;
        }

        List<VhostTuple> changes = new LinkedList<>();
        for ( VhostTuple t : vhostTuples() )
        {
            if ( !t.oldVhost.equals( t.newVhost ) )
            {
                changes.add( t );
            }
        }
        return changes;
    }

    @Value.Derived
    protected List<VhostTuple> vhostTuples()
    {
        if ( oldDeployment().isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List<VhostTuple> res = new LinkedList<>();
        for ( Vhost oldVhost : oldDeployment().get().spec().vHosts() )
        {
            Optional<Vhost> newVhost = newDeployment().spec().vHosts().stream().filter( h -> h.host().equals( oldVhost.host() ) ).findAny();
            if ( newVhost.isPresent() )
            {
                res.add( new VhostTuple( oldVhost, newVhost.get() ) );
            }
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

    public static class VhostTuple
    {
        private final Optional<Vhost> oldVhost;

        private final Vhost newVhost;

        protected VhostTuple( final Vhost oldVhost, final Vhost newVhost )
        {
            this.oldVhost = Optional.ofNullable( oldVhost );
            this.newVhost = newVhost;
        }

        public static VhostTuple of( final Vhost oldVhost, final Vhost newVhost )
        {
            return new VhostTuple( oldVhost, newVhost );
        }

        public Optional<Vhost> getOldVhost()
        {
            return oldVhost;
        }

        public Vhost getNewVhost()
        {
            return newVhost;
        }

        @Override
        public String toString()
        {
            return newVhost.host();
        }
    }
}
