//package com.enonic.ec.kubernetes.operator.api.admission;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//import java.util.function.Consumer;
//
//import javax.enterprise.context.ApplicationScoped;
//import javax.inject.Inject;
//import javax.ws.rs.Consumes;
//import javax.ws.rs.POST;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//
//import org.eclipse.microprofile.config.inject.ConfigProperty;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.google.common.base.Preconditions;
//
//import io.fabric8.kubernetes.api.model.HasMetadata;
//import io.fabric8.kubernetes.api.model.Status;
//import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
//import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
//
//import com.enonic.ec.kubernetes.operator.api.BaseApi;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.crd.Xp7AppResource;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.ImmutableInfoXp7App;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info.ImmutableInfoXp7Config;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client.Xp7DeploymentCache;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.ImmutableInfoXp7Deployment;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.client.Xp7VHostCache;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.ImmutableInfoXp7VHost;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.InfoXp7VHost;
//
//@ApplicationScoped
//@Path("/apis/operator.enonic.cloud/v1alpha1")
//public class AdmissionApi
//    extends BaseApi<AdmissionReview>
//{
//    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );
//
//    @ConfigProperty(name = "operator.deployment.xp.allNodes")
//    String allNodesPicker;
//
//    @Inject
//    Xp7DeploymentCache xp7DeploymentCache;
//
//    @Inject
//    Xp7VHostCache xp7VHostCache;
//
//    Map<Class<? extends HasMetadata>, Consumer<AdmissionReview>> admissionFunctionMap;
//
//    public AdmissionApi()
//    {
//        admissionFunctionMap = new HashMap<>();
//        admissionFunctionMap.put( Xp7DeploymentResource.class, this::xpDeploymentReview );
//        admissionFunctionMap.put( Xp7VHostResource.class, this::xpVHostReview );
//        admissionFunctionMap.put( Xp7ConfigResource.class, this::xpConfigReview );
//        admissionFunctionMap.put( Xp7AppResource.class, this::xpAppReview );
//    }
//
//    @POST
//    @Path("/validations")
//    @Consumes("application/json")
//    @Produces("application/json")
//    public AdmissionReview validate( String request )
//        throws IOException
//    {
//        log.debug( "Admission review: " + request );
//        return getResult( request, AdmissionReview.class );
//    }
//
//    @Override
//    protected AdmissionReview process( final String uid, final AdmissionReview review )
//    {
//        if ( review.getRequest().getOperation().equals( "CREATE" ) || review.getRequest().getOperation().equals( "UPDATE" ) )
//        {
//            HasMetadata obj = review.getRequest().getOldObject();
//            if ( review.getRequest().getObject() != null )
//            {
//                obj = review.getRequest().getObject();
//            }
//            Consumer<AdmissionReview> func = admissionFunctionMap.get( obj.getClass() );
//            Preconditions.checkState( func != null, "Admission review sent to endpoint that has unknown object" );
//            func.accept( review );
//        }
//        return createReview( uid, null );
//    }
//
//    @Override
//    protected AdmissionReview failure( final String uid, final String message )
//    {
//        return createReview( uid, message );
//    }
//
//    public AdmissionReview createReview( String uid, String errorMessage )
//    {
//        AdmissionReview review = new AdmissionReview();
//        AdmissionResponse response = new AdmissionResponse();
//        review.setResponse( response );
//
//        response.setUid( uid );
//
//        Status status = new Status();
//        response.setStatus( status );
//
//        if ( errorMessage == null )
//        {
//            response.setAllowed( true );
//            return review;
//        }
//
//        response.setAllowed( false );
//        status.setCode( 400 );
//        status.setMessage( errorMessage );
//
//        return review;
//    }
//
//    private void xpDeploymentReview( final AdmissionReview review )
//    {
//        ImmutableInfoXp7Deployment.builder().
//            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7DeploymentResource) obj ) ).
//            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7DeploymentResource) obj ) ).
//            build();
//    }
//
//    private void xpVHostReview( final AdmissionReview review )
//    {
//        InfoXp7VHost vHost = ImmutableInfoXp7VHost.builder().
//            xpDeploymentCache( xp7DeploymentCache ).
//            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7VHostResource) obj ) ).
//            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7VHostResource) obj ) ).
//            build();
//        if ( !vHost.resource().getSpec().skipIngress() )
//        {
//            long sameHost = xp7VHostCache.stream().
//                filter( r -> !r.getMetadata().getUid().equals( vHost.resource().getMetadata().getUid() ) ).
//                filter( r -> !r.getSpec().skipIngress() ).
//                filter( r -> r.getSpec().host().equals( vHost.resource().getSpec().host() ) ).
//                count();
//            Preconditions.checkState( sameHost < 1L, "This host is being used by another Xp7VHost" );
//        }
//    }
//
//    private void xpConfigReview( final AdmissionReview review )
//    {
//        ImmutableInfoXp7Config.builder().
//            xpDeploymentCache( xp7DeploymentCache ).
//            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7ConfigResource) obj ) ).
//            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7ConfigResource) obj ) ).
//            build();
//    }
//
//    private void xpAppReview( final AdmissionReview review )
//    {
//        ImmutableInfoXp7App.builder().
//            xpDeploymentCache( xp7DeploymentCache ).
//            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (Xp7AppResource) obj ) ).
//            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (Xp7AppResource) obj ) ).
//            build();
//    }
//}
