package com.enonic.cloud.operator.api.admission;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.operator.api.BaseApi;
import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.v1alpha1.xp7app.info.ImmutableInfoXp7App;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.ImmutableInfoXp7Config;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.InfoXp7Config;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7VHost;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.InfoXp7VHost;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class AdmissionApi
    extends BaseApi<AdmissionReview>
{
    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );

    private final Map<Class<? extends HasMetadata>, Consumer<AdmissionReview>> admissionFunctionMap;

    @ConfigProperty(name = "operator.helm.charts.Values.allNodesKey")
    String allNodesPicker;

    @Inject
    Caches caches;

    public AdmissionApi()
    {
        admissionFunctionMap = new HashMap<>();
        admissionFunctionMap.put( V1alpha2Xp7Deployment.class, this::xpDeploymentReview );
        admissionFunctionMap.put( V1alpha2Xp7VHost.class, this::xpVHostReview );
        admissionFunctionMap.put( V1alpha2Xp7Config.class, this::xpConfigReview );
        admissionFunctionMap.put( V1alpha1Xp7App.class, this::xpAppReview );
    }

    @POST
    @Path("/validations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview validate( String request )
        throws IOException
    {
        log.debug( "Admission review: " + request );
        return getResult( request, AdmissionReview.class );
    }

    @Override
    protected AdmissionReview process( final String uid, final AdmissionReview review )
    {
        if ( review.getRequest().getOperation().equals( "CREATE" ) || review.getRequest().getOperation().equals( "UPDATE" ) )
        {
            HasMetadata obj = review.getRequest().getOldObject();
            if ( review.getRequest().getObject() != null )
            {
                obj = review.getRequest().getObject();
            }
            Consumer<AdmissionReview> func = admissionFunctionMap.get( obj.getClass() );
            Preconditions.checkState( func != null, "Admission review sent to endpoint that has unknown object" );
            func.accept( review );
        }
        return createReview( uid, null );
    }

    @Override
    protected AdmissionReview failure( final String uid, final String message )
    {
        return createReview( uid, message );
    }

    private AdmissionReview createReview( String uid, String errorMessage )
    {
        AdmissionReview review = new AdmissionReview();
        AdmissionResponse response = new AdmissionResponse();
        review.setResponse( response );

        response.setUid( uid );

        Status status = new Status();
        response.setStatus( status );

        if ( errorMessage == null )
        {
            response.setAllowed( true );
            return review;
        }

        response.setAllowed( false );
        status.setCode( 400 );
        status.setMessage( errorMessage );

        return review;
    }

    private void xpDeploymentReview( final AdmissionReview review )
    {
        InfoXp7Deployment deployment = ImmutableInfoXp7Deployment.builder().
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (V1alpha2Xp7Deployment) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (V1alpha2Xp7Deployment) obj ) ).
            build();
        deployment.resource().getSpec().nodeGroups().keySet().forEach(
            nodeGroup -> Preconditions.checkState( !allNodesPicker.equals( nodeGroup ), "Node groups cannot be named '" + allNodesPicker +
                "' because that is the identifier for all nodeGroups" ) );
    }

    private void xpVHostReview( final AdmissionReview review )
    {
        InfoXp7VHost vHost = ImmutableInfoXp7VHost.builder().
            caches( caches ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (V1alpha2Xp7VHost) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (V1alpha2Xp7VHost) obj ) ).
            build();

        for ( V1alpha2Xp7VHostSpecMapping mapping : vHost.resource().getSpec().mappings() )
        {
            if ( !cfgStr( "operator.helm.charts.Values.allNodesKey" ).equals( mapping.nodeGroup() ) )
            {
                Preconditions.checkState( vHost.deploymentInfo().resource().getSpec().nodeGroups().containsKey( mapping.nodeGroup() ),
                                          String.format( "Xp7Deployment '%s' does not contain nodeGroup '%s'",
                                                         vHost.deploymentInfo().deploymentName(), mapping.nodeGroup() ) );
            }
        }

        if ( !vHost.resource().getSpec().skipIngress() )
        {
            long sameHost = caches.getVHostCache().getCollection().stream().
                filter( r -> !r.getMetadata().getUid().equals( vHost.resource().getMetadata().getUid() ) ).
                filter( r -> !r.getSpec().skipIngress() ).
                filter( r -> r.getSpec().host().equals( vHost.resource().getSpec().host() ) ).
                count();
            Preconditions.checkState( sameHost < 1L, "This host is being used by another Xp7VHost" );
        }
    }

    private void xpConfigReview( final AdmissionReview review )
    {
        InfoXp7Config config = ImmutableInfoXp7Config.builder().
            caches( caches ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (V1alpha2Xp7Config) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (V1alpha2Xp7Config) obj ) ).
            build();

        if ( !cfgStr( "operator.helm.charts.Values.allNodesKey" ).equals( config.resource().getSpec().nodeGroup() ) )
        {
            String nodeGroup = config.resource().getSpec().nodeGroup();
            Preconditions.checkState( config.deploymentInfo().resource().getSpec().nodeGroups().containsKey( nodeGroup ),
                                      String.format( "Xp7Deployment '%s' does not contain nodeGroup '%s'",
                                                     config.deploymentInfo().deploymentName(), nodeGroup ) );
        }
    }

    private void xpAppReview( final AdmissionReview review )
    {
        ImmutableInfoXp7App.builder().
            caches( caches ).
            oldResource( Optional.ofNullable( review.getRequest().getOldObject() ).map( obj -> (V1alpha1Xp7App) obj ) ).
            newResource( Optional.ofNullable( review.getRequest().getObject() ).map( obj -> (V1alpha1Xp7App) obj ) ).
            build();
    }
}
