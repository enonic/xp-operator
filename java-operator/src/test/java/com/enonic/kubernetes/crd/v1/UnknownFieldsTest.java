package com.enonic.kubernetes.crd.v1;

import io.fabric8.kubernetes.client.utils.KubernetesSerialization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownFieldsTest
{
    private static final String DEPLOYMENT = """
        {
          "apiVersion": "enonic.cloud/v1",
          "kind": "Xp8Deployment",
          "metadata": {"name": "test", "namespace": "test"},
          "spec": {
            "enabled": true,
            "xpVersion": "8.2.0",
            "nodesPreinstalledApps": [],
            "nodeGroups": [{"name": "main", "replicas": 1, "futureField": "x"}]
          },
          "status": {"state": "RUNNING", "message": "OK", "futureStatus": 1}
        }
        """;

    private static final String CONFIG = """
        {
          "apiVersion": "enonic.cloud/v1",
          "kind": "Xp8Config",
          "metadata": {"name": "test", "namespace": "test"},
          "spec": {"nodeGroup": "all", "file": "a.cfg", "data": "", "futureField": true}
        }
        """;

    private final KubernetesSerialization serialization = new KubernetesSerialization();

    @Test
    void deploymentWithUnknownFields()
    {
        final Xp8Deployment deployment = serialization.unmarshal( DEPLOYMENT, Xp8Deployment.class );

        assertThat( deployment.getSpec().getXpVersion() ).isEqualTo( "8.2.0" );
        assertThat( deployment.getSpec().getNodeGroups().getFirst().getName() ).isEqualTo( "main" );
        assertThat( deployment.getStatus().getMessage() ).isEqualTo( "OK" );
    }

    @Test
    void deploymentKeepsUnknownFields()
    {
        final Xp8Deployment deployment = serialization.unmarshal( DEPLOYMENT, Xp8Deployment.class );

        final String json = serialization.asJson( deployment );

        assertThat( json ).contains( "\"nodesPreinstalledApps\":[]", "\"futureField\":\"x\"", "\"futureStatus\":1" );
    }

    @Test
    void configWithUnknownFields()
    {
        final Xp8Config config = serialization.unmarshal( CONFIG, Xp8Config.class );

        assertThat( config.getSpec().getFile() ).isEqualTo( "a.cfg" );
        assertThat( serialization.asJson( config ) ).contains( "\"futureField\":true" );
    }
}
