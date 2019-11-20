package com.enonic.ec.kubernetes.crd.deployment.spec;

import org.junit.jupiter.api.Test;

import com.enonic.ec.kubernetes.DeploymentFileTest;

class SpecTest
    extends DeploymentFileTest
{

    @Test
    public void specValidMinimal()
    {
        loadResource( "specValidMinimal.yaml" );
    }

    @Test
    public void specValidFull()
    {
        loadResource( "specValidFull.yaml" );
    }

    @Test
    public void specInvalid()
    {
        loadSpecExpectIllegalState( "specInvalid.yaml", "Some fields in 'spec' are missing: [xpVersion, enabled, sharedDisk]" );
    }

    @Test
    public void specInvalidAdditionalField()
    {
        loadSpecExpectIllegalState( "specInvalidAdditionalField.yaml", "Field unrecognized: asdf" );
    }

    @Test
    public void specInvalidNodes()
    {
        loadSpecExpectIllegalState( "specInvalidNodes.yaml", "Some fields in 'spec.nodes' are missing: [name, replicas, resources]" );
    }

    @Test
    public void specInvalidNodesNone()
    {
        loadSpecExpectIllegalState( "specInvalidNodesNone.yaml", "Field 'spec.nodes' has to contain more than 0 nodes" );
    }
}