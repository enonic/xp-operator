package com.enonic.ec.kubernetes.operator.crd.xp7deployment.spec;

import java.util.Map;
import java.util.Set;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7DeploymentSpecNode.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7DeploymentSpecNode.ExceptionMissing.class, throwForNullPointer = Xp7DeploymentSpecNode.ExceptionMissing.class)
public abstract class Xp7DeploymentSpecNode
    extends Configuration
{
    @Nullable
    public abstract String displayName();

    public abstract Integer replicas();

    public abstract Set<Type> type();

    public abstract Xp7DeploymentSpecNodeResources resources();

    public abstract Map<String, String> env();

    public abstract Map<String, String> config();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( type().size() > 0, "Field 'spec.nodes.type' has to be set" );
        Preconditions.checkState( replicas() > 0, "Field 'spec.nodes.replicas' has to be > 0" );

        Preconditions.checkState( !isDataNode() || resources().disks().containsKey( "index" ),
                                  "Nodes with type " + Type.DATA + " must have disk 'index' defined" );
        Preconditions.checkState( !isDataNode() || resources().disks().size() == 1,
                                  "Nodes with type " + Type.DATA + " should only have 'index' disk defined" );
        Preconditions.checkState( isDataNode() || resources().disks().size() == 0,
                                  "Nodes with type " + Type.MASTER + " or " + Type.FRONTEND + " should not have any disks defined" );

        Preconditions.checkState( !isMasterNode() || replicas() % 2 == 1,
                                  "Nodes with type " + Type.MASTER + " replicas has to be an odd number" );
    }

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

    public enum Type
    {
        FRONTEND, DATA, MASTER
    }

    public static class ExceptionMissing
        extends BuilderException
    {

        public ExceptionMissing( final String... missingAttributes )
        {
            super( missingAttributes );
        }

        @Override
        protected String getFieldPath()
        {
            return "spec.nodes";
        }
    }
}
