package com.enonic.cloud.operator.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;


public abstract class BaseAdmissionApi<R>
{
    private final static Logger log = LoggerFactory.getLogger( BaseAdmissionApi.class );

    private final Map<Class<? extends HasMetadata>, Consumer<R>> functionMap;

    @Inject
    public ObjectMapper mapper;

    @Inject
    public InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher;

    @ConfigProperty(name = "operator.api.debug")
    Boolean debug;

    protected BaseAdmissionApi()
    {
        functionMap = new HashMap<>();
    }

    protected void addFunction( Class<? extends HasMetadata> k, Consumer<R> func )
    {
        functionMap.put( k, func );
    }

    public AdmissionReview handle( AdmissionReview admissionReview )
        throws JsonProcessingException
    {
        String apiName = this.getClass().getSimpleName();
        if ( debug )
        {
            log.info(
                String.format( "%s Request: %s", apiName, mapper.writerWithDefaultPrettyPrinter().writeValueAsString( admissionReview ) ) );
        }

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
                func.accept( apiObject );
            }
            catch ( Throwable e )
            {
                error = e.getMessage();
            }
        }

        AdmissionResponseBuilder builder = new AdmissionResponseBuilder().
            withUid( admissionReview.getRequest().getUid() ).
            withAllowed( error == null );

        if ( error != null )
        {
            builder.withStatus( new StatusBuilder().withMessage( error ).build() );
            log.error( String.format( "%s failed with error: %s", apiName, error ) );
        }
        else if ( apiObject != null )
        {
            postRequestHook( apiObject, builder );
        }

        admissionReview.setResponse( builder.build() );
        if ( debug )
        {
            log.info( String.format( "%s Response: %s", apiName,
                                     mapper.writerWithDefaultPrettyPrinter().writeValueAsString( admissionReview ) ) );
        }
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

    protected HasMetadata getObject( final AdmissionReview admissionReview )
    {
        if ( admissionReview.getRequest().getObject() != null )
        {
            return admissionReview.getRequest().getObject();
        }
        return admissionReview.getRequest().getOldObject();
    }

    protected abstract R createApiObject( final AdmissionReview admissionReview );

    protected List<Xp7Deployment> getXp7Deployment( HasMetadata hasMetadata )
    {
        return xp7DeploymentInformerSearcher.get( hasMetadata.getMetadata().getNamespace() ).collect( Collectors.toList() );
    }
}
