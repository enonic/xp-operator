package com.enonic.ec.kubernetes.operator.operators.v1alpha1.api.info;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    @ConfigProperty(name = "operator.api.group")
    String group;

    @ConfigProperty(name = "operator.api.apiVersion")
    String apiVersion;

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
        admissionResource.put( "version", apiVersion );

        Map<String, Object> res = new HashMap<>();
        res.put( "apiVersion", "v1" );
        res.put( "groupVersion", group + "/" + apiVersion );
        res.put( "kind", "APIResourceList" );
        res.put( "resources", Collections.singletonList( admissionResource ) );
        return res;
    }

    @GET
    @Path("/internal/version")
    @Produces("application/json")
    public Map<String, String> info()
        throws IOException
    {
        Properties git = new Properties();
        git.load( getClass().getClassLoader().getResourceAsStream( "git.properties" ) );
        Map<String, String> res = new HashMap<>();

        res.put( "buildDate", git.getProperty( "git.build.time" ) );
        res.put( "gitCommit", git.getProperty( "git.commit.id" ) );
        res.put( "gitTreeState", git.getProperty( "git.dirty" ).equals( "true" ) ? "dirty" : "clean" );
        res.put( "version", git.getProperty( "git.build.version" ) );
        res.put( "gitTags", git.getProperty( "git.tags" ) );

        return res;
    }
}
