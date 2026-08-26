package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.FieldsV1;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.internal.PatchUtils;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;

import org.junit.jupiter.api.Test;

/**
 * Standalone repro for the NPE seen in production: patching a Secret whose metadata carries a
 * populated managedFields[].fieldsV1 (server-side-apply tracking, present on any real fetched
 * Secret) triggers a Jackson NPE inside fabric8's JSON-diff patch machinery.
 */
public class JacksonPatchReproTest
{
    @Test
    public void patchSecretWithManagedFields()
    {
        Secret oldSecret = new Secret();
        oldSecret.setMetadata( managedMeta() );

        Secret newSecret = new Secret();
        newSecret.setMetadata( managedMeta() );
        newSecret.setData( Map.of( "key", "dmFsdWU=" ) );

        PatchUtils.jsonDiff( oldSecret, newSecret, true, new KubernetesSerialization() );
    }

    private ObjectMeta managedMeta()
    {
        FieldsV1 fieldsV1 = new FieldsV1();
        fieldsV1.setAdditionalProperty( "f:metadata", Map.of( "f:labels", Map.of() ) );

        ManagedFieldsEntry entry = new ManagedFieldsEntry();
        entry.setManager( "kubectl" );
        entry.setOperation( "Update" );
        entry.setFieldsV1( fieldsV1 );

        ObjectMeta meta = new ObjectMeta();
        meta.setName( "test-secret" );
        meta.setManagedFields( List.of( entry ) );
        return meta;
    }
}
