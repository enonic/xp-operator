package com.enonic.kubernetes.client.v1.api.operator.v1;

import com.enonic.kubernetes.client.v1.api.operator.OperatorVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

@EnableKubernetesMockClient
public class OperatorApiImplTest
{
    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void operatorVersionRequest() throws IOException {

        final KubernetesClient namespacedKubernetesClient = kubernetesClient;
        final Config configuration = namespacedKubernetesClient.getConfiguration();

        final OperatorVersion operatorVersion = loadSchema( "operatorVersion.json" );

        server.expect().get().withPath("/apis/operator.enonic.cloud/v1/operator/version")
            .andReturn( HttpURLConnection.HTTP_ACCEPTED, Serialization.asJson( operatorVersion ) )
            .once();

        final OperatorApiImpl operatorApi = new OperatorApiImpl( namespacedKubernetesClient.getHttpClient(), configuration );

        Assertions.assertEquals( operatorVersion, operatorApi.version());
    }

    private OperatorVersion loadSchema( final String file )
        throws IOException
    {
        Map<String, Object> map = mapper.readValue( getClass().getResource( file ), Map.class );
        cleanMap( map );
        return mapper.readValue( mapper.writeValueAsString( map ), OperatorVersion.class );
    }

    private void cleanMap( Map<String, Object> map )
    {
        map.remove( "extends" );
        map.remove( "javaName" );
        for (Object o : map.values()) {
            if (o instanceof Map) {
                cleanMap( (Map<String, Object>) o );
            }
        }
    }
}
