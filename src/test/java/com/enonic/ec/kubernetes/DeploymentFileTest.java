package com.enonic.ec.kubernetes;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.enonic.ec.kubernetes.deployment.XpDeploymentResource;

public class DeploymentFileTest
{
    public static ObjectMapper mapper = new ObjectMapper( new YAMLFactory() );

    protected XpDeploymentResource loadResource( String file )
    {
        return loadResource( this.getClass(), file );
    }

    protected XpDeploymentResource loadResource( Class klass, String file )
    {
        try
        {
            return mapper.readValue( klass.getResourceAsStream( file ), XpDeploymentResource.class );
        }
        catch ( IOException ioex )
        {
            throw new RuntimeException( ioex );
        }
    }

    protected void loadSpecExpectIllegalState( String file, String expectedMessage )
    {
        RuntimeException e = Assertions.assertThrows( RuntimeException.class, () -> loadResource( file ) );
        Assertions.assertEquals( expectedMessage, e.getCause().getCause().getMessage() );
    }
}
