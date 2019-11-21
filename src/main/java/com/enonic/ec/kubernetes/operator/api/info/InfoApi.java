package com.enonic.ec.kubernetes.operator.api.info;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1alpha1")
public class InfoApi
{

    @GET
    @Path("/")
    @Produces("application/json")
    public List<String> paths()
    {
        return Arrays.asList( "/version" );
    }

    @GET
    @Path("/version")
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
