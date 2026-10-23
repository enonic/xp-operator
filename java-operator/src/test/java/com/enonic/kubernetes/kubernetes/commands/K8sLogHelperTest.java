package com.enonic.kubernetes.kubernetes.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnableKubernetesMockClient(crud = true)
public class K8sLogHelperTest
{
    KubernetesClient client;

    /**
     * Another writer changes the resource between the read and the write of an edit, as cloud-api
     * patches an Xp8Config's data while the operator sets its status. The edit must land on top of
     * that change, not put back the copy it read.
     */
    @Test
    public void editKeepsAChangeMadeAfterItsRead()
    {
        client.configMaps().inNamespace( "ns" ).resource(
            new ConfigMapBuilder().withNewMetadata().withName( "cfg" ).endMetadata().addToData( "file", "old" ).build() ).create();
        final Resource<ConfigMap> resource = client.configMaps().inNamespace( "ns" ).withName( "cfg" );

        final AtomicInteger calls = new AtomicInteger();
        K8sLogHelper.logEdit( resource, c -> {
            if ( calls.getAndIncrement() == 0 )
            {
                resource.edit( other -> {
                    other.getData().put( "file", "new" );
                    return other;
                } );
            }
            final Map<String, String> labels = c.getMetadata().getLabels() == null ? new HashMap<>() : c.getMetadata().getLabels();
            labels.put( "loaded", "true" );
            c.getMetadata().setLabels( labels );
            return c;
        } );

        final ConfigMap stored = resource.get();
        assertEquals( "new", stored.getData().get( "file" ) );
        assertEquals( "true", stored.getMetadata().getLabels().get( "loaded" ) );
        assertEquals( 2, calls.get() );
    }
}
