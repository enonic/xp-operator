package com.enonic.ec.kubernetes.deployment.xpdeployment;

import org.junit.jupiter.api.Test;

import com.enonic.ec.kubernetes.DeploymentFileTest;

class XpDeploymentSpecValidationTest
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
        loadSpecExpectIllegalState( "specInvalid.yaml",
                                    "Cannot build Spec, some of required attributes are not set [xpVersion, cloud, project, name, enabled, sharedDisks]" );
    }

    @Test
    public void specInvalidApp()
    {
        loadSpecExpectIllegalState( "specInvalidApp.yaml", "field app has to be 'xp' for xp deployments" );
    }

    @Test
    public void specInvalidNodes()
    {
        loadSpecExpectIllegalState( "specInvalidNodes.yaml",
                                    "Cannot build XpDeploymentResourceSpecNode, some of required attributes are not set [alias, replicas, type, resources]" );
    }

    @Test
    public void specInvalidNodesNone()
    {
        loadSpecExpectIllegalState( "specInvalidNodesNone.yaml", "field 'nodes' has to contain more than 0 nodes" );
    }

    @Test
    public void specInvalidNodesDoubleStandalone()
    {
        loadSpecExpectIllegalState( "specInvalidNodesDoubleStandalone.yaml",
                                    "you can only have one node there is a node of type " + " " +
                                        " in the node list" );
    }

    // TODO: When cluster is implemented, test for same STANDALONE + MASTER/FRONT/DATA nodes

    @Test
    public void specInvalidNodesUnsupported()
    {
        loadSpecExpectIllegalState( "specInvalidNodesUnsupported.yaml",
                                    "Operator only supports nodes of type " + " " );
    }

    @Test
    public void specInvalidNodeStandalone()
    {
        loadSpecExpectIllegalState( "specInvalidNodeStandalone.yaml", "field replicas on node type STANDALONE has to be less than 2" );
    }

    @Test
    public void specInvalidNodeResources()
    {
        loadSpecExpectIllegalState( "specInvalidNodeResources.yaml",
                                    "Cannot build XpDeploymentResourceSpecNodeResources, some of required attributes are not set [cpu, memory]" );
    }

    @Test
    public void specInvalidNodeResourcesDisks1()
    {
        loadSpecExpectIllegalState( "specInvalidNodeResourcesDisks1.yaml", "field resources.disks.index on node has to be set" );
    }

    @Test
    public void specInvalidNodeResourcesDisks2()
    {
        loadSpecExpectIllegalState( "specInvalidNodeResourcesDisks2.yaml", "field resources.disks.snapshots on node has to be set" );
    }

    @Test
    public void specInvalidVHost()
    {
        loadSpecExpectIllegalState( "specInvalidVHost.yaml",
                                    "Cannot build XpDeploymentResourceSpecVHostCertificate, some of required attributes are not set [selfSigned]" );
    }

    // TODO: When cluster is implemented, test for same vhost + path on multiple nodes
}