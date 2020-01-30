package com.enonic.ec.kubernetes.operator.api.info;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class InfoApi
{
    @ConfigProperty(name = "operator.api.version")
    String apiVersion;

    @ConfigProperty(name = "operator.api.group")
    String group;

    @GET
    @Path("/")
    @Produces("application/json")
    public Map<String, Object> api()
    {
        Map<String, Object> admissionResource = new HashMap<>();
        admissionResource.put( "group", "admission.k8s.io" );
        admissionResource.put( "kind", "AdmissionReview" );
        admissionResource.put( "name", "validations" );
        admissionResource.put( "namespaced", false );
        admissionResource.put( "singularName", "" );
        admissionResource.put( "verbs", Collections.singletonList( "create" ) );
        admissionResource.put( "version", "v1beta1" );

        Map<String, Object> conversionResource = new HashMap<>();
        conversionResource.put( "group", "apiextensions.k8s.io" );
        admissionResource.put( "kind", "ConversionReview" );
        admissionResource.put( "name", "conversions" );
        admissionResource.put( "namespaced", false );
        admissionResource.put( "singularName", "" );
        admissionResource.put( "verbs", Collections.singletonList( "create" ) );
        admissionResource.put( "version", "v1beta1" );

        Map<String, Object> res = new HashMap<>();
        res.put( "apiVersion", "v1" );
        res.put( "groupVersion", group + "/" + apiVersion );
        res.put( "kind", "APIResourceList" );
        res.put( "resources", Arrays.asList( admissionResource, conversionResource ) );
        return res;
    }

    @GET
    @Path("/internal/version")
    @Produces("application/json")
    public Map<String, String> info()
        throws IOException
    {
        Properties git = new Properties();
        git.load( Objects.requireNonNull( getClass().getClassLoader().getResourceAsStream( "git.properties" ) ) );
        Map<String, String> res = new HashMap<>();

        res.put( "buildDate", git.getProperty( "git.build.time" ) );
        res.put( "gitCommit", git.getProperty( "git.commit.id" ) );
        res.put( "gitTreeState", git.getProperty( "git.dirty" ).equals( "true" ) ? "dirty" : "clean" );
        res.put( "version", git.getProperty( "git.build.version" ) );
        res.put( "gitTags", git.getProperty( "git.tags" ) );

        return res;
    }
}
