package com.enonic.ec.kubernetes.operator.api.conversion;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.operator.api.BaseApi;
import com.enonic.ec.kubernetes.operator.api.conversion.model.ConversionReview;
import com.enonic.ec.kubernetes.operator.api.conversion.model.ImmutableConversionReview;
import com.enonic.ec.kubernetes.operator.api.conversion.model.ImmutableConversionReviewResponse;
import com.enonic.ec.kubernetes.operator.api.conversion.model.ImmutableConversionReviewResponseResult;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class ConversionApi
    extends BaseApi<ConversionReview>
{
    private final static Logger log = LoggerFactory.getLogger( ConversionApi.class );

    Map<Class<? extends HasMetadata>, Function<HasMetadata, HasMetadata>> conversionFunctionMap;

    public ConversionApi()
    {
        conversionFunctionMap = new HashMap<>();
    }

    @POST
    @Path("/conversions")
    @Consumes("application/json")
    @Produces("application/json")
    public ConversionReview convert( String request )
        throws IOException
    {
        log.debug( "Conversion review: " + request );
        return getResult( request, ConversionReview.class );
    }

    @Override
    protected ConversionReview process( final String uid, final ConversionReview review )
    {
        if ( !"enonic.cloud/v1alpha1".equals( review.request().desiredAPIVersion() ) )
        {
            return failure( uid, "Api cannot convert '" + review.request().desiredAPIVersion() + "'" );
        }
        List<HasMetadata> resultingObjects = new LinkedList<>();
        for ( HasMetadata obj : review.request().objects() )
        {
            try
            {
                resultingObjects.add( convertObject( obj ) );
            }
            catch ( Exception e )
            {
                return failure( uid, e.getMessage() );
            }
        }
        return ImmutableConversionReview.builder().
            response( ImmutableConversionReviewResponse.builder().
                uid( uid ).
                result( ImmutableConversionReviewResponseResult.builder().
                    status( "Success" ).
                    build() ).
                build() ).
            build();
    }

    @Override
    protected ConversionReview failure( final String uid, final String message )
    {
        return ImmutableConversionReview.builder().
            response( ImmutableConversionReviewResponse.builder().
                uid( uid ).
                result( ImmutableConversionReviewResponseResult.builder().
                    status( "Failed" ).
                    message( message ).
                    build() ).
                build() ).
            build();
    }

    private HasMetadata convertObject( final HasMetadata obj )
    {
        Function<HasMetadata, HasMetadata> func = conversionFunctionMap.get( obj.getClass() );
        Preconditions.checkState( func != null, String.format( "No conversion function for apiVersion: %s, kind: %s ", obj.getApiVersion(),
                                                               obj.getKind() ) );
        return func.apply( obj );
    }
}
