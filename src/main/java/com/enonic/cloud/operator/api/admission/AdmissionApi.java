package com.enonic.cloud.operator.api.admission;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7DeploymentCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7VHostCache;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;
import com.enonic.cloud.operator.api.BaseApi;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Validator.dns1035;
import static com.enonic.cloud.common.Validator.dns1123;

@SuppressWarnings("ConstantConditions")
@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class AdmissionApi
    extends BaseApi<AdmissionReview>
{
    private final static Logger log = LoggerFactory.getLogger( AdmissionApi.class );

    private final static String specMissing = "Old and new resource can not be empty, is 'spec' missing?";

    private final static String deploymentMissing = "Xp7Deployment '%s' not found";

    private final static String missingNodeGroup = "Xp7Deployment '%s' does not contain nodeGroup '%s'";

    private final Map<Class<? extends HasMetadata>, Consumer<AdmissionReview>> admissionFunctionMap;

    @ConfigProperty(name = "operator.helm.charts.Values.allNodesKey")
    String allNodesPicker;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7DeploymentCache v1alpha2Xp7DeploymentCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7VHostCache v1alpha2Xp7VHostCache;

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
    protected AdmissionReview createOnFailure( final Map<String, Object> request )
    {
        AdmissionReview admissionReview = new AdmissionReview();
        admissionReview.setApiVersion( (String) request.get( "apiVersion" ) );
        admissionReview.setKind( (String) request.get( "kind" ) );
        admissionReview.setRequest( new AdmissionRequest() );
        admissionReview.getRequest().setUid( (String) ( (Map) request.get( "request" ) ).get( "uid" ) );
        return admissionReview;
    }

    @Override
    protected AdmissionReview process( final AdmissionReview review )
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
        return createReview( review, null );
    }

    @Override
    protected AdmissionReview failure( final AdmissionReview review, final String message )
    {
        return createReview( review, message );
    }

    private AdmissionReview createReview( final AdmissionReview review, String errorMessage )
    {
        AdmissionResponse response = new AdmissionResponse();
        review.setResponse( response );

        response.setUid( review.getRequest().getUid() );

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
        Optional<V1alpha2Xp7Deployment> o = Optional.ofNullable( review.getRequest().getOldObject() ).map( r -> (V1alpha2Xp7Deployment) r );
        Optional<V1alpha2Xp7Deployment> n = Optional.ofNullable( review.getRequest().getObject() ).map( r -> (V1alpha2Xp7Deployment) r );

        o.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );
        n.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );

        V1alpha2Xp7Deployment r = n.orElse( o.orElse( null ) );

        if ( r.getMetadata().getDeletionTimestamp() != null )
        {
            return;
        }

        r.getSpec().nodeGroups().keySet().forEach( k -> dns1123( "nodeId", k ) );

        r.getSpec().nodeGroups().keySet().forEach( nodeGroup -> Preconditions.checkState( !allNodesPicker.equals( nodeGroup ),
                                                                                          "Node groups cannot be named '" + allNodesPicker +
                                                                                              "' because that is the identifier for all nodeGroups" ) );

        cfgIfBool( "operator.deployment.xp.labels.strictValidation", () -> {
            Preconditions.checkState( r.ecCloud() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ), r.ecCloud() );

            Preconditions.checkState( r.ecSolution() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.solution" ) +
                                          "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.solution" ), r.ecSolution() );

            Preconditions.checkState( r.ecEnvironment() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.environment" ) +
                                          "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.environment" ), r.ecEnvironment() );

            Preconditions.checkState( r.ecService() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.service" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.service" ), r.ecService() );

            String fullName = String.join( "-", r.ecCloud(), r.ecSolution(), r.ecEnvironment(), r.ecService() );
            Preconditions.checkState( r.getMetadata().getName().equals( fullName ),
                                      "Xp7Deployment name must be equal to <Cloud>-<Solution>-<Environment>-<Service> according to labels, i.e: '" +
                                          fullName + "'" );
        } );

        if ( o.isPresent() && n.isPresent() )
        {
            Preconditions.checkState( o.get().getSpec().nodesSharedDisks().equals( n.get().getSpec().nodesSharedDisks() ),
                                      "Field 'spec.nodesSharedDisk' cannot be changed" );

            Set<String> sameNodes = new HashSet<>( o.get().getSpec().nodeGroups().keySet() );
            sameNodes.retainAll( o.get().getSpec().nodeGroups().keySet() );
            for ( String node : sameNodes )
            {
                Preconditions.checkState( o.get().getSpec().nodeGroups().get( node ).resources().disks().equals(
                    n.get().getSpec().nodeGroups().get( node ).resources().disks() ),
                                          "Field 'spec.nodes.resources.disks' cannot be changed" );
            }
        }
    }

    private void xpVHostReview( final AdmissionReview review )
    {
        Optional<V1alpha2Xp7VHost> o = Optional.ofNullable( review.getRequest().getOldObject() ).map( r -> (V1alpha2Xp7VHost) r );
        Optional<V1alpha2Xp7VHost> n = Optional.ofNullable( review.getRequest().getObject() ).map( r -> (V1alpha2Xp7VHost) r );

        o.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );
        n.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );

        V1alpha2Xp7VHost r = n.orElse( o.orElse( null ) );

        if ( r.getMetadata().getDeletionTimestamp() != null )
        {
            return;
        }

        Optional<V1alpha2Xp7Deployment> deployment =
            v1alpha2Xp7DeploymentCache.get( r.getMetadata().getNamespace(), r.getMetadata().getNamespace() );
        Preconditions.checkState( deployment.isPresent(), String.format( deploymentMissing, r.getMetadata().getNamespace() ) );

        for ( V1alpha2Xp7VHostSpecMapping mapping : r.getSpec().mappings() )
        {
            if ( !cfgStr( "operator.helm.charts.Values.allNodesKey" ).equals( mapping.nodeGroup() ) )
            {
                Preconditions.checkState( deployment.get().getSpec().nodeGroups().containsKey( mapping.nodeGroup() ),
                                          String.format( "Xp7Deployment '%s' does not contain nodeGroup '%s'",
                                                         deployment.get().getMetadata().getName(), mapping.nodeGroup() ) );
            }
        }


        if ( r.getSpec().hasIngress() )
        {
            long sameHost = v1alpha2Xp7VHostCache.getStream().
                filter( v -> !v.getMetadata().getUid().equals( r.getMetadata().getUid() ) ).
                filter( v -> v.getSpec().hasIngress() ).
                filter( v -> v.getSpec().host().equals( r.getSpec().host() ) ).
                count();
            Preconditions.checkState( sameHost < 1L, "This host is being used by another Xp7VHost" );
        }
    }

    private void xpConfigReview( final AdmissionReview review )
    {
        Optional<V1alpha2Xp7Config> o = Optional.ofNullable( review.getRequest().getOldObject() ).map( r -> (V1alpha2Xp7Config) r );
        Optional<V1alpha2Xp7Config> n = Optional.ofNullable( review.getRequest().getObject() ).map( r -> (V1alpha2Xp7Config) r );

        o.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );
        n.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );

        V1alpha2Xp7Config r = n.orElse( o.orElse( null ) );
        if ( r.getMetadata().getDeletionTimestamp() != null )
        {
            return;
        }
        Optional<V1alpha2Xp7Deployment> deployment =
            v1alpha2Xp7DeploymentCache.get( r.getMetadata().getNamespace(), r.getMetadata().getNamespace() );
        Preconditions.checkState( deployment.isPresent(), String.format( deploymentMissing, r.getMetadata().getNamespace() ) );

        deployment.ifPresent( d -> {
            if ( !r.getSpec().nodeGroup().equals( cfgStr( "operator.helm.charts.Values.allNodesKey" ) ) )
            {
                Preconditions.checkState( deployment.get().getSpec().nodeGroups().containsKey( r.getSpec().nodeGroup() ),
                                          String.format( missingNodeGroup, d.getMetadata().getName(), r.getSpec().nodeGroup() ) );

            }
        } );
    }

    private void xpAppReview( final AdmissionReview review )
    {
        Optional<V1alpha1Xp7App> o = Optional.ofNullable( review.getRequest().getOldObject() ).map( r -> (V1alpha1Xp7App) r );
        Optional<V1alpha1Xp7App> n = Optional.ofNullable( review.getRequest().getObject() ).map( r -> (V1alpha1Xp7App) r );

        o.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );
        n.ifPresent( r -> Preconditions.checkState( r.getSpec() != null, specMissing ) );

        V1alpha1Xp7App r = n.orElse( o.orElse( null ) );
        if ( r.getMetadata().getDeletionTimestamp() != null )
        {
            return;
        }
        Optional<V1alpha2Xp7Deployment> deployment =
            v1alpha2Xp7DeploymentCache.get( r.getMetadata().getNamespace(), r.getMetadata().getNamespace() );
        Preconditions.checkState( deployment.isPresent(), String.format( deploymentMissing, r.getMetadata().getNamespace() ) );
    }
}
