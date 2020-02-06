package com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost;

import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpecMappingIdProviders.Builder.class)
@Value.Immutable
@Value.Style(additionalJsonAnnotations = {
    JsonProperty.class}, throwForInvalidImmutableState = V1alpha2Xp7VHostSpecMappingIdProviders.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpecMappingIdProviders.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpecMappingIdProviders
{
    @JsonProperty("default")
    public abstract String defaultIdProvider();

    public abstract Set<String> enabled();

    public static class ExceptionMissing
        extends BuilderException
    {

        public ExceptionMissing( final String... missingAttributes )
        {
            super( replaceWithCorrectName( missingAttributes ) );
        }

        private static String[] replaceWithCorrectName( String[] missingAttributes )
        {
            for ( int i = 0; i < missingAttributes.length; i++ )
            {
                if ( missingAttributes[i].equals( "defaultIdProvider" ) )
                {
                    missingAttributes[i] = "default";
                }
            }
            return missingAttributes;
        }

        @Override
        protected String getFieldPath()
        {
            return "spec.mappings.idProviders";
        }
    }
}
