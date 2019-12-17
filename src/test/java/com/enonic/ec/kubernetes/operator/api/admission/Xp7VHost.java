package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Xp7VHost
    extends TestAdmissionApi
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7vhost/create/invalidCertificate.yaml", "Field 'spec.certificate' cannot be set if ingress is skipped" );
            tests.put( "xp7vhost/create/invalidMappings.yaml", "Field 'spec.mappings.target' has to be unique" );
            tests.put( "xp7vhost/create/missingHost.yaml", "Some fields in 'spec' are missing: [host]" );
            tests.put( "xp7vhost/create/missingMapping.yaml", "Field 'spec.mappings' has to contain more than 0 mappings" );
            tests.put( "xp7vhost/create/missingMappingIdProvider.yaml", null );
            tests.put( "xp7vhost/create/missingMappingNode.yaml", "Some fields in 'spec.mappings' are missing: [node]" );
            tests.put( "xp7vhost/create/missingMappingSource.yaml", "Some fields in 'spec.mappings' are missing: [source]" );
            tests.put( "xp7vhost/create/missingMappingTarget.yaml", "Some fields in 'spec.mappings' are missing: [target]" );
            tests.put( "xp7vhost/create/missingSpec.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "xp7vhost/create/valid.yaml", null );
            tests.put( "xp7vhost/create/validAll.yaml", null );
            tests.put( "xp7vhost/create/validFull.yaml", null );
            tests.put( "xp7vhost/create/wrongAuthority.yaml",
                       "Cannot deserialize value of type `com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.Xp7VHostSpecCertificateAuthority` from String \"wrong\": value not one of declared Enum instance names: [selfSigned, letsEncrypt, letsEncryptStaging]  at [Source: UNKNOWN; line: -1, column: -1] (through reference chain: io.fabric8.kubernetes.api.model.admission.AdmissionReview[\"request\"]->io.fabric8.kubernetes.api.model.admission.AdmissionRequest[\"object\"]->com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource[\"spec\"]->com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.ImmutableXp7VHostSpec$Builder[\"certificate\"]->com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.ImmutableXp7VHostSpecCertificate$Builder[\"authority\"])" );
            tests.put( "xp7vhost/create/wrongNamespace.yaml", "XpDeployment 'wrongnamespace' not found" );
            tests.put( "xp7vhost/create/wrongNode.yaml", "XpDeployment 'mycloud-myproject-qaxp' does not contain node 'wrong'" );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7vhost/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7vhost/delete/valid.yaml", null );
        } );
    }
}
