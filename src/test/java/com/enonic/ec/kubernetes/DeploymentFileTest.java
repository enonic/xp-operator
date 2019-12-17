package com.enonic.ec.kubernetes;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.enonic.ec.kubernetes.operator.api.admission.AdmissionExceptionHandler;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResource;

public class DeploymentFileTest
{
    public static ObjectMapper mapper = new ObjectMapper( new YAMLFactory() );

    protected Xp7DeploymentResource loadResource( String file )
    {
        return loadResource( this.getClass(), file );
    }

    protected Xp7DeploymentResource loadResource( Class klass, String file )
    {
        try
        {
            return mapper.readValue( klass.getResourceAsStream( file ), Xp7DeploymentResource.class );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( AdmissionExceptionHandler.extractJacksonMessage( ex ) );
        }
    }

    protected void loadSpecExpectIllegalState( String file, String expectedMessage )
    {
        RuntimeException e = Assertions.assertThrows( RuntimeException.class, () -> loadResource( file ) );
        Assertions.assertEquals( expectedMessage, e.getMessage() );
    }
}
