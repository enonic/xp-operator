package com.enonic.cloud.operator.api.conversion;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.api.BaseApi;
import com.enonic.cloud.operator.api.conversion.converters.Converter;
import com.enonic.cloud.operator.api.conversion.model.ConversionReview;
import com.enonic.cloud.operator.api.conversion.model.ImmutableConversionReview;
import com.enonic.cloud.operator.api.conversion.model.ImmutableConversionReviewResponse;
import com.enonic.cloud.operator.api.conversion.model.ImmutableConversionReviewResponseResult;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class ConversionApi
    extends BaseApi<ConversionReview>
{
    private final static Logger log = LoggerFactory.getLogger( ConversionApi.class );

    private final Map<String, Map<Class<? extends HasMetadata>, Converter>> conversionFunctionMap;

    @SuppressWarnings("unchecked")
    public ConversionApi()
    {
        conversionFunctionMap = new HashMap<>();

        List<Converter> converters = new LinkedList<>();
//        converters.add( new V1alpha1ToV1alpha2() );
//        converters.add( new V1alpha2ToV1alpha1() );

        converters.forEach( c -> {
            Map<Class<? extends HasMetadata>, Converter> vMap = conversionFunctionMap.getOrDefault( c.produces(), new HashMap<>() );
            vMap.put( c.consumes(), c );
            conversionFunctionMap.put( c.produces(), vMap );
        } );
    }

    @POST
    @Path("/conversions")
    @Consumes("application/json")
    @Produces("application/json")
    public ConversionReview convert( String request )
        throws IOException
    {
        log.debug( "Conversion review: " + request.trim() );
        return getResult( request, ConversionReview.class );
    }

    @Override
    protected ConversionReview process( final String uid, final ConversionReview review )
    {
        Map<Class<? extends HasMetadata>, Converter> convertFuncMap = conversionFunctionMap.get( review.request().desiredAPIVersion() );
        Preconditions.checkState( convertFuncMap != null, "No conversion functions for '" + review.request().desiredAPIVersion() + "'" );
        List<HasMetadata> resultingObjects = new LinkedList<>();
        for ( HasMetadata obj : review.request().objects() )
        {
            try
            {
                resultingObjects.add( convertObject( convertFuncMap, obj ) );
            }
            catch ( Exception e )
            {
                return failure( uid, e.getMessage() );
            }
        }
        return ImmutableConversionReview.copyOf( review ).
            withResponse( ImmutableConversionReviewResponse.builder().
                uid( uid ).
                result( ImmutableConversionReviewResponseResult.builder().
                    status( "Success" ).
                    build() ).
                convertedObjects( resultingObjects ).
                build() );
    }

    @Override
    protected ConversionReview failure( final String uid, final String message )
    {
        return ImmutableConversionReview.builder().
            kind( "ConversionReview" ).
            apiVersion( "apiextensions.k8s.io/v1beta1" ).
            response( ImmutableConversionReviewResponse.builder().
                uid( uid ).
                result( ImmutableConversionReviewResponseResult.builder().
                    status( "Failed" ).
                    message( message ).
                    build() ).
                build() ).
            build();
    }

    @SuppressWarnings("unchecked")
    private HasMetadata convertObject( final Map<Class<? extends HasMetadata>, Converter> convertFuncMap, final HasMetadata obj )
    {
        Converter converter = convertFuncMap.get( obj.getClass() );
        Preconditions.checkState( converter != null,
                                  String.format( "No conversion function for apiVersion: %s, kind: %s ", obj.getApiVersion(),
                                                 obj.getKind() ) );
        return converter.convert( obj );
    }
}
