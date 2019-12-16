package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class VHosts
    extends AdmissionApiTest
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "vhosts/create/invalidCertificate.yaml", "Field 'spec.certificate' cannot be set if ingress is skipped" );
            tests.put( "vhosts/create/invalidMappings.yaml", "Field 'spec.mappings.target' has to be unique" );
            tests.put( "vhosts/create/missingHost.yaml", "Some fields in 'spec' are missing: [host]" );
            tests.put( "vhosts/create/missingMapping.yaml", "Field 'spec.mappings' has to contain more than 0 mappings" );
            tests.put( "vhosts/create/missingMappingIdProvider.yaml", null );
            tests.put( "vhosts/create/missingMappingNode.yaml", "Some fields in 'spec.mappings' are missing: [node]" );
            tests.put( "vhosts/create/missingMappingSource.yaml", "Some fields in 'spec.mappings' are missing: [source]" );
            tests.put( "vhosts/create/missingMappingTarget.yaml", "Some fields in 'spec.mappings' are missing: [target]" );
            tests.put( "vhosts/create/valid.yaml", null );
            tests.put( "vhosts/create/validFull.yaml", null );
            tests.put( "vhosts/create/wrongAuthority.yaml",
                       "Cannot deserialize value of type `com.enonic.ec.kubernetes.operator.crd.vhost.spec.SpecCertificateAuthority` from String \"wrong\": value not one of declared Enum instance names: [selfSigned, letsEncrypt, letsEncryptStaging]  at [Source: UNKNOWN; line: -1, column: -1] (through reference chain: io.fabric8.kubernetes.api.model.admission.AdmissionReview[\"request\"]->io.fabric8.kubernetes.api.model.admission.AdmissionRequest[\"object\"]->com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource[\"spec\"]->com.enonic.ec.kubernetes.operator.crd.vhost.spec.ImmutableSpec$Builder[\"certificate\"]->com.enonic.ec.kubernetes.operator.crd.vhost.spec.ImmutableSpecCertificate$Builder[\"authority\"])" );
            tests.put( "vhosts/create/wrongNamespace.yaml",
                       "Xp7VHost can only be created in namespaces that are created by Xp7Deployment" );
            tests.put( "vhosts/create/wrongNode.yaml",
                       "Field 'spec.mappings.node' with value 'wrong' has to match a node in a Xp7Deployment" );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "vhosts/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "vhosts/delete/valid.yaml", null );
        } );
    }
}
