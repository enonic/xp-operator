package com.enonic.cloud.operator.api.mutation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha2")
public class MutationApi
{
    private final static Logger log = LoggerFactory.getLogger( MutationApi.class );

    @Inject
    public ObjectMapper mapper;

    @ConfigProperty(name = "operator.api.debug")
    Boolean debug;

    private final Map<Class<? extends HasMetadata>, Function<AdmissionReview, List<Patch>>> functionMap;

    public MutationApi()
    {
        functionMap = new HashMap<>();
        functionMap.put( Xp7App.class, this::xp7app );
        functionMap.put( Xp7Config.class, this::xp7config );
        functionMap.put( Xp7Deployment.class, this::xp7deployment );
        functionMap.put( Xp7VHost.class, this::xp7VHost );
    }

    @POST
    @Path("/mutations")
    @Consumes("application/json")
    @Produces("application/json")
    public AdmissionReview mutate( AdmissionReview admissionReview )
        throws JsonProcessingException
    {
        String type = String.format( "%s/%s", admissionReview.getRequest().getObject().getApiVersion(),
                                     admissionReview.getRequest().getObject().getKind() );
        log.debug( String.format( "MutationApi called with type %s", type ) );

        if ( debug )
        {
            log.info( "MutationApi Request: %s", mapper.writeValueAsString( admissionReview ) );
        }

        Function<AdmissionReview, List<Patch>> func = functionMap.get( admissionReview.getRequest().getObject() );
        String error = null;

        if ( func == null )
        {
            error = String.format( "MutationApi cannot handle resources of type %s", type );
        }

        List<Patch> patches = null;
        if ( error != null )
        {
            try
            {
                patches = func.apply( admissionReview );
                if ( patches != null && patches.isEmpty() )
                {
                    patches = null;
                }
            }
            catch ( Throwable e )
            {
                error = e.getMessage();
            }
        }

        AdmissionResponseBuilder builder = new AdmissionResponseBuilder().
            withUid( admissionReview.getRequest().getUid() );

        if ( patches != null )
        {
            try
            {
                builder.withPatch( mapper.writeValueAsString( patches ) );
            }
            catch ( JsonProcessingException e )
            {
                error = e.getMessage();
            }
        }

        builder.withAllowed( error == null );

        if ( error != null )
        {
            builder.withNewStatus().withMessage( error ).endStatus();
            log.error( String.format( "MutationApi failed with error: %s", error ) );
        }

        admissionReview.setResponse( builder.build() );
        if ( debug )
        {
            log.info( "MutationApi Response: %s", mapper.writeValueAsString( admissionReview ) );
        }
        return admissionReview;
    }

    public List<Patch> xp7app( AdmissionReview admissionReview )
    {
        return null;
    }

    public List<Patch> xp7config( AdmissionReview admissionReview )
    {
        return null;
    }

    public List<Patch> xp7deployment( AdmissionReview admissionReview )
    {
        return null;
    }

    public List<Patch> xp7VHost( AdmissionReview admissionReview )
    {
        return null;
    }
}
