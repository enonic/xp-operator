package com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostStatusFields.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostStatusFields.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostStatusFields.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostStatusFields
{
    public abstract List<String> ipsAssigned();

    @Value.Default
    public boolean dnsRecordCreated()
    {
        return false;
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
            return "status.fields";
        }
    }
}
