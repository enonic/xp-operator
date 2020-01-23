//package com.enonic.ec.kubernetes.operator.api.conversion.converters.xpdeployments;
//
//import java.util.Map;
//
//import com.enonic.ec.kubernetes.operator.api.conversion.converters.Converter;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.ImmutableXp7DeploymentSpec;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.ImmutableXp7DeploymentSpecNode;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.ImmutableXp7DeploymentSpecNodeResources;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.crd.spec.Xp7DeploymentSpecNode;
//
//public class V1alpha2ToV1alpha1
//    implements
//    Converter<com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource, com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource>
//{
//    @Override
//    public Class<com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource> consumes()
//    {
//        return com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource.class;
//    }
//
//    @Override
//    public String produces()
//    {
//        return "enonic.cloud/v1alpha1";
//    }
//
//    @Override
//    public com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource convert(
//        final com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.crd.Xp7DeploymentResource source )
//    {
//        ImmutableXp7DeploymentSpec.Builder builder = ImmutableXp7DeploymentSpec.builder().
//            enabled( source.getSpec().enabled() ).
//            xpVersion( source.getSpec().xpVersion() ).
//            nodesSharedDisk( source.getSpec().nodesSharedDisks().values().stream().findFirst().get() );
//
//        for ( Map.Entry<String, Xp7DeploymentSpecNode> e : source.getSpec().nodeGroups().entrySet() )
//        {
//            ImmutableXp7DeploymentSpecNode.Builder nodeBuilder = ImmutableXp7DeploymentSpecNode.builder().
//                displayName( e.getValue().displayName() ).
//                replicas( e.getValue().replicas() ).
//                resources( ImmutableXp7DeploymentSpecNodeResources.builder().
//                    cpu( e.getValue().resources().cpu() ).
//                    memory( e.getValue().resources().memory() ).
//                    disks( e.getValue().resources().disks() ).
//                    build() );
//            if ( e.getValue().master() )
//            {
//                nodeBuilder.addType(
//                    com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode.Type.MASTER );
//            }
//            if ( e.getValue().data() )
//            {
//                nodeBuilder.addType(
//                    com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode.Type.DATA );
//            }
//            if ( !e.getValue().data() && !e.getValue().master() )
//            {
//                nodeBuilder.addType(
//                    com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode.Type.FRONTEND );
//            }
//
//            builder.putNodes( e.getKey(), nodeBuilder.build() );
//        }
//
//        com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource result =
//            new com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource();
//        result.setSpec( builder.build() );
//        result.setKind( source.getKind() );
//        result.setMetadata( source.getMetadata() );
//        result.setApiVersion( produces() );
//
//        return result;
//    }
//}
