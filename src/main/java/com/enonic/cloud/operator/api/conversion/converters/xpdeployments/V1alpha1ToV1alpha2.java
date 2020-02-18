//package com.enonic.cloud.operator.api.conversion.converters.xpdeployments;
//
//import java.util.Map;
//
//import com.enonic.cloud.operator.api.conversion.converters.Converter;
//import com.enonic.cloud.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;
//import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource;
//import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.crd.spec.ImmutableXp7DeploymentSpec;
//import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.crd.spec.ImmutableXp7DeploymentSpecNode;
//import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.crd.spec.ImmutableXp7DeploymentSpecNodeResources;
//
//
//public class V1alpha1ToV1alpha2
//    implements
//    Converter<com.enonic.cloud.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource, com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource>
//{
//    @Override
//    public Class<com.enonic.cloud.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource> consumes()
//    {
//        return com.enonic.cloud.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource.class;
//    }
//
//    @Override
//    public String produces()
//    {
//        return "enonic.cloud/v1alpha2";
//    }
//
//    @Override
//    public com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource convert(
//        final com.enonic.cloud.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource source )
//    {
//        ImmutableXp7DeploymentSpec.Builder builder = ImmutableXp7DeploymentSpec.builder().
//            enabled( source.getSpec().enabled() ).
//            xpVersion( source.getSpec().xpVersion() ).
//            putNodesSharedDisks( "blobstore", source.getSpec().nodesSharedDisk() ).
//            putNodesSharedDisks( "snapshots", source.getSpec().nodesSharedDisk() );
//
//        for ( Map.Entry<String, Xp7DeploymentSpecNode> e : source.getSpec().nodes().entrySet() )
//        {
//            builder.putNodeGroups( e.getKey(), ImmutableXp7DeploymentSpecNode.builder().
//                displayName( e.getValue().displayName() ).
//                data( e.getValue().isDataNode() ).
//                master( e.getValue().isMasterNode() ).
//                replicas( e.getValue().replicas() ).
//                resources( ImmutableXp7DeploymentSpecNodeResources.builder().
//                    cpu( e.getValue().resources().cpu() ).
//                    memory( e.getValue().resources().memory() ).
//                    disks( e.getValue().resources().disks() ).
//                    build() ).
//                env( e.getValue().env() ).
//                build() );
//        }
//
//        Xp7DeploymentResource result = new Xp7DeploymentResource();
//        result.setSpec( builder.build() );
//        result.setKind( source.getKind() );
//        result.setMetadata( source.getMetadata() );
//        result.setApiVersion( produces() );
//        return result;
//    }
//}
