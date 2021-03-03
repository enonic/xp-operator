package com.enonic.cloud.operator.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.kubernetes.Predicates.inSameNamespace;


public abstract class BaseAdmissionApi<R>
{
    private final static Logger log = LoggerFactory.getLogger( BaseAdmissionApi.class );

    private final Map<Class<? extends HasMetadata>, Consumer<R>> functionMap;

    @Inject
    public ObjectMapper mapper;

    @Inject
    public Searchers searchers;

    public BaseAdmissionApi()
    {
        this.functionMap = new HashMap<>();
    }

    protected void addFunction( Class<? extends HasMetadata> k, Consumer<R> func )
    {
        functionMap.put( k, func );
    }

    protected AdmissionReview handle( AdmissionReview admissionReview )
        throws JsonProcessingException
    {
        String apiName = this.getClass().getSimpleName();
        cfgIfBool( "operator.api.debug", () -> {
            try
            {
                log.info( String.format( "%s Request: %s", apiName,
                                         mapper.writerWithDefaultPrettyPrinter().writeValueAsString( admissionReview ) ) );
            }
            catch ( JsonProcessingException e )
            {
                // Ignore
            }
        } );

        HasMetadata obj = getObject( admissionReview );

        String type = String.format( "%s/%s", obj.getApiVersion(), obj.getKind() );
        log.debug( String.format( "%s called with type %s", apiName, type ) );

        Consumer<R> func = functionMap.get( obj.getClass() );
        String error = null;

        if ( func == null )
        {
            error = String.format( "%s cannot handle resources of type %s", apiName, type );
        }

        R apiObject = null;
        if ( error == null && !deleteEvent( admissionReview ) )
        {
            try
            {
                apiObject = createApiObject( admissionReview );
                Objects.requireNonNull( func ).accept( apiObject );
            }
            catch ( Throwable e )
            {
                error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
        }

        AdmissionResponseBuilder builder = new AdmissionResponseBuilder().
            withUid( admissionReview.getRequest().getUid() ).
            withAllowed( error == null );

        StatusBuilder statusBuilder = new StatusBuilder();
        if ( error != null )
        {
            statusBuilder.withMessage( error );
            log.warn( String.format( "%s failed for %s %s in NS %s: %s", apiName, obj.getKind(), obj.getMetadata().getName(),
                                     obj.getMetadata().getNamespace(), error ) );
        }
        else if ( apiObject != null )
        {
            postRequestHook( apiObject, builder );
        }
        builder.withStatus( statusBuilder.build() );

        admissionReview.setResponse( builder.build() );
        cfgIfBool( "operator.api.debug", () -> {
            try
            {
                log.info( String.format( "%s Response: %s", apiName,
                                         mapper.writerWithDefaultPrettyPrinter().writeValueAsString( admissionReview ) ) );
            }
            catch ( JsonProcessingException e )
            {
                // Ignore
            }
        } );
        return admissionReview;
    }

    private boolean deleteEvent( final AdmissionReview admissionReview )
    {
        return admissionReview.getRequest().getOperation().equals( "DELETE" );
    }

    protected void postRequestHook( final R apiObject, final AdmissionResponseBuilder builder )
        throws JsonProcessingException
    {
        // Do nothing
    }

    private HasMetadata getObject( final AdmissionReview admissionReview )
    {
        if ( admissionReview.getRequest().getObject() != null )
        {
            return (HasMetadata) admissionReview.getRequest().getObject();
        }
        return (HasMetadata) admissionReview.getRequest().getOldObject();
    }

    protected abstract R createApiObject( final AdmissionReview admissionReview );

    protected Optional<Xp7Deployment> getXp7Deployment( KubernetesResource resource )
    {
        return searchers.xp7Deployment().stream().
            filter( inSameNamespace( (HasMetadata) resource ) ).
            findFirst();
    }

    protected AdmissionOperation getOperation( AdmissionReview r )
    {
        return AdmissionOperation.valueOf( r.getRequest().getOperation() );
    }
}
