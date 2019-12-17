package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Deployments
    extends AdmissionApiTest
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStream( tests -> {
            tests.put( "deployments/create/missingEnabled.yaml", "Some fields in 'spec' are missing: [enabled]" );
            tests.put( "deployments/create/missingLabelCloud.yaml", "Label 'metadata.labels.cloud' is missing" );
            tests.put( "deployments/create/missingLabelName.yaml", "Label 'metadata.labels.name' is missing" );
            tests.put( "deployments/create/missingLabelProject.yaml", "Label 'metadata.labels.project' is missing" );
            tests.put( "deployments/create/missingNodeResources.yaml", "Some fields in 'spec.nodes' are missing: [resources]" );
            tests.put( "deployments/create/missingNodeResourcesCpu.yaml", "Some fields in 'spec.nodes.resources' are missing: [cpu]" );
            tests.put( "deployments/create/missingNodeResourcesDisks.yaml", "Nodes with type DATA must have disk 'index' defined" );
            tests.put( "deployments/create/missingNodeResourcesMemory.yaml",
                       "Some fields in 'spec.nodes.resources' are missing: [memory]" );
            tests.put( "deployments/create/missingNodes.yaml", "Field 'spec.nodes' has to contain more than 0 nodes" );
            tests.put( "deployments/create/missingNodeType.yaml", "Field 'spec.nodes.type' has to be set" );
            tests.put( "deployments/create/missingReplicas.yaml", "Some fields in 'spec.nodes' are missing: [replicas]" );
            tests.put( "deployments/create/missingSharedDisk.yaml", "Some fields in 'spec' are missing: [nodesSharedDisk]" );
            tests.put( "deployments/create/missingSpec.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "deployments/create/missingVersion.yaml", "Some fields in 'spec' are missing: [xpVersion]" );
            tests.put( "deployments/create/valid.yaml", null );
            tests.put( "deployments/create/validCluster.yaml", null );
            tests.put( "deployments/create/validFull.yaml", null );
            tests.put( "deployments/create/wrongClusterDisk1.yaml",
                       "Nodes with type MASTER or FRONTEND should not have any disks defined" );
            tests.put( "deployments/create/wrongClusterDisk2.yaml", "Nodes with type DATA must have disk 'index' defined" );
            tests.put( "deployments/create/wrongClusterDisk3.yaml",
                       "Nodes with type MASTER or FRONTEND should not have any disks defined" );
            tests.put( "deployments/create/wrongClusterNodes1.yaml", "1 and only 1 node has be of type DATA" );
            tests.put( "deployments/create/wrongClusterNodes2.yaml", "1 and only 1 node has be of type MASTER" );
            tests.put( "deployments/create/wrongName.yaml",
                       "Xp7Deployment name must be equal to <Cloud>-<Project>-<Name> according to labels, i.e: 'mycloud-myproject-qaxp'" );
            tests.put( "deployments/create/wrongNodeName.yaml",
                       "Field 'nodeId' with value '1234' is not valid, it must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name',  or 'abc-123', regex used for validation is '[a-z]([-a-z0-9]*[a-z0-9])?')" );
            tests.put( "deployments/create/wrongNodeReplicas1.yaml", "Field 'spec.nodes.replicas' has to be > 0" );
            tests.put( "deployments/create/wrongNodeReplicas2.yaml", "Field 'spec.nodes.replicas' has to be > 0" );
            tests.put( "deployments/create/wrongNodeType.yaml",
                       "Cannot deserialize value of type `com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode$Type` from String \"WRONG\": value not one of declared Enum instance names: [FRONTEND, MASTER, DATA]  at [Source: UNKNOWN; line: -1, column: -1] (through reference chain: io.fabric8.kubernetes.api.model.admission.AdmissionReview[\"request\"]->io.fabric8.kubernetes.api.model.admission.AdmissionRequest[\"object\"]->com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource[\"spec\"]->com.enonic.ec.kubernetes.operator.crd.deployment.spec.ImmutableSpec$Builder[\"nodes\"]->java.util.LinkedHashMap[\"main\"]->com.enonic.ec.kubernetes.operator.crd.deployment.spec.ImmutableSpecNode$Builder[\"type\"]->java.util.ArrayList[3])" );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStream( tests -> {
            tests.put( "deployments/update/changeCloud.yaml", "Field 'metadata.labels.cloud' cannot be changed" );
            tests.put( "deployments/update/changeName.yaml", "Field 'metadata.labels.name' cannot be changed" );
            tests.put( "deployments/update/changeNodeDisk.yaml", "Field 'spec.nodes.resources.disks' cannot be changed" );
            tests.put( "deployments/update/changeNodeSharedDisk.yaml", "Field 'spec.nodesSharedDisk' cannot be changed" );
            tests.put( "deployments/update/changeProject.yaml", "Field 'metadata.labels.project' cannot be changed" );
            tests.put( "deployments/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStream( tests -> {
            tests.put( "deployments/delete/valid.yaml", null );
        } );
    }
}
