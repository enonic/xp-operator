package com.enonic.ec.kubernetes.crd.deployment.spec;

import java.util.Map;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Configuration;

@JsonDeserialize(builder = ImmutableSpecNode.Builder.class)
@Value.Immutable
public abstract class SpecNode
    extends Configuration
{
    public enum Type
    {
        FRONTEND, DATA, MASTER
    }

    public abstract String alias();

    public abstract Integer replicas();

    public abstract Set<Type> type();

    public abstract SpecNodeResources resources();

    public abstract Map<String, String> env();

    @Value.Default
    public SpecNodeConfig config()
    {
        return new SpecNodeConfig();
    }

    //region class consistency check

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( type().size() > 0, "node 'type' has to be set" );
        Preconditions.checkState( replicas() > 0, "node 'replicas' has to be > 0" );

        Preconditions.checkState( !isDataNode() || resources().disks().containsKey( "index" ),
                                  "data nodes must have 'index' disk defined" );
        Preconditions.checkState( !isDataNode() || resources().disks().size() == 1, "data node should only have 'index' disk defined" );
        Preconditions.checkState( isDataNode() || resources().disks().size() == 0,
                                  "master and frontend nodes should not have any disk defined" );

        Preconditions.checkState( !isMasterNode() || replicas() % 2 == 1, "master node replica number has to be an odd number" );
    }

    //endregion

    //region derived functions

    private boolean isType( Type... type )
    {
        for ( Type t : type )
        {
            if ( type().contains( t ) )
            {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    @Value.Derived
    public boolean isMasterNode()
    {
        return isType( Type.MASTER );
    }

    @JsonIgnore
    @Value.Derived
    public boolean isFrontendNode()
    {
        return isType( Type.FRONTEND );
    }

    @JsonIgnore
    @Value.Derived
    public boolean isDataNode()
    {
        return isType( Type.DATA );
    }

    @JsonIgnore
    @Value.Derived
    public boolean isOnlyFrontend()
    {
        return isType( Type.FRONTEND ) && type().size() == 1;
    }

    @JsonIgnore
    @Value.Derived
    public Map<String, String> nodeAliasLabel()
    {
        // Do not add more labels here, it will brake service mapping for vHosts to pods
        return Map.of( cfgStr( "operator.deployment.xp.pod.label.aliasPrefix" ) + alias(), alias() );
    }

    //endregion
}
