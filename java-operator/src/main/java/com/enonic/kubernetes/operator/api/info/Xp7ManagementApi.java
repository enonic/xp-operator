package com.enonic.kubernetes.operator.api.info;

import com.enonic.kubernetes.apis.xp.XpClient;
import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.XpClientCacheKeyImpl;
import com.enonic.kubernetes.apis.xp.XpClientException;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.apis.xp.service.AppInstallRequest;
import com.enonic.kubernetes.apis.xp.service.AppInstallResponse;
import com.enonic.kubernetes.apis.xp.service.AppKey;
import com.enonic.kubernetes.client.v1.api.xp7.idproviders.Xp7MgmtIdProvider;
import com.enonic.kubernetes.client.v1.api.xp7.projects.Xp7MgmtProject;
import com.enonic.kubernetes.client.v1.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import com.enonic.kubernetes.client.v1.api.xp7.webapps.Xp7MgmtWebapp;
import com.enonic.kubernetes.crd.v1.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Searchers;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.kubernetes.Predicates.withName;
import static com.enonic.kubernetes.operator.xp7deployment.Predicates.running;
import static com.enonic.kubernetes.operator.xp7deployment.Predicates.withNodeGroup;

@Controller("/apis/operator.enonic.cloud/v1")
public class Xp7ManagementApi
{

    public static Logger logger = LoggerFactory.getLogger( Xp7ManagementApi.class );

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    private KubernetesClientException k8sException( int code, String message )
    {
        return new KubernetesClientException( message, code,
                                              new StatusBuilder().withCode( code ).withMessage( message ).withReason( message ).build() );
    }

    private XpClient getClient( final String namespace, final String name, final String nodeGroup )
        throws XpClientException
    {
        Optional<Xp7Deployment> xp7Deployment = searchers.xp7Deployment().find( inNamespace( namespace ), withName( name ) );

        if ( xp7Deployment.isEmpty() )
        {
            throw k8sException( 404, String.format( "No Xp7Deployment in namespace '%s' with name '%s' not found", namespace, name ) );
        }

        if ( !withNodeGroup( nodeGroup ).test( xp7Deployment.get() ) )
        {
            throw k8sException( 404,
                                String.format( "No Xp7Deployment in namespace '%s' with name '%s' does not have nodegroup %s", namespace,
                                               name, nodeGroup ) );
        }

        if ( !running().test( xp7Deployment.get() ) )
        {
            throw k8sException( 503, "Xp7Deployment not running" );
        }

        return xpClientCache.getClient( XpClientCacheKeyImpl.of( namespace, name, nodeGroup ) );
    }

    @Get("/xp7/{namespace}/{name}/{nodegroup}/mgmt/repo/snapshots/list")
    @Produces("application/json")
    public Xp7MgmtSnapshotsList snapshotList( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                                              @PathVariable("nodegroup") final String nodeGroup )
        throws KubernetesClientException
    {
        try
        {
            final Xp7MgmtSnapshotsList xp7MgmtSnapshotsList = getClient( namespace, name, nodeGroup ).snapshotsList();
            logger.warn(new ObjectMapper().writeValueAsString( xp7MgmtSnapshotsList ));
            return xp7MgmtSnapshotsList;
        }
        catch ( Exception e )
        {
            logger.error( "Failed listing snapshots", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Get("/xp7/{namespace}/{name}/{nodegroup}/mgmt/idproviders/list")
    @Produces("application/json")
    public List<Xp7MgmtIdProvider> idProvidersList( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                                                    @PathVariable("nodegroup") final String nodeGroup )
        throws KubernetesClientException
    {
        try
        {
            return getClient( namespace, name, nodeGroup ).idProvidersList();
        }
        catch ( Exception e )
        {
            logger.error( "Failed listing idproviders", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Get("/xp7/{namespace}/{name}/{nodegroup}/mgmt/content/projects/list")
    @Produces("application/json")
    public List<Xp7MgmtProject> projectsList( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                                              @PathVariable("nodegroup") final String nodeGroup )
        throws KubernetesClientException
    {
        try
        {
            return getClient( namespace, name, nodeGroup ).projectsList();
        }
        catch ( Exception e )
        {
            logger.error( "Failed listing projects", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Get("/xp7/{namespace}/{name}/{nodegroup}/mgmt/webapps/list")
    @Produces("application/json")
    public List<Xp7MgmtWebapp> webappsList( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                                            @PathVariable("nodegroup") final String nodeGroup )
        throws KubernetesClientException
    {
        try
        {
            return getClient( namespace, name, nodeGroup ).webappsList();
        }
        catch ( Exception e )
        {
            logger.error( "Failed listing webapps", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Get("/xp7/{namespace}/{name}/{nodegroup}/mgmt/apps/list")
    @Produces("application/json")
    public List<AppInfo> appsList( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                                   @PathVariable("nodegroup") final String nodeGroup )
        throws KubernetesClientException
    {
        try
        {
            return getClient( namespace, name, nodeGroup ).appList();
        }
        catch ( Exception e )
        {
            logger.error( "Failed listing apps", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Post("/xp7/{namespace}/{name}/{nodegroup}/mgmt/apps/install")
    @Consumes("application/json")
    @Produces("application/json")
    public AppInstallResponse appInstall( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                                          @PathVariable("nodegroup") final String nodeGroup, @Body final AppInstallRequest request )
        throws KubernetesClientException
    {
        try
        {
            return getClient( namespace, name, nodeGroup ).appInstall( request );
        }
        catch ( Exception e )
        {
            logger.error( "Failed installing app", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Post("/xp7/{namespace}/{name}/{nodegroup}/mgmt/apps/uninstall")
    @Consumes("application/json")
    public void appUninstall( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                              @PathVariable("nodegroup") final String nodeGroup, @Body final AppKey key )
        throws KubernetesClientException
    {
        try
        {
            getClient( namespace, name, nodeGroup ).appUninstall( key );
        }
        catch ( Exception e )
        {
            logger.error( "Failed uninstalling app", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Post("/xp7/{namespace}/{name}/{nodegroup}/mgmt/apps/start")
    @Consumes("application/json")
    public void appStart( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                          @PathVariable("nodegroup") final String nodeGroup, @Body final AppKey key )
        throws KubernetesClientException
    {
        try
        {
            getClient( namespace, name, nodeGroup ).appStart( key );
        }
        catch ( Exception e )
        {
            logger.error( "Failed starting app", e );
            throw k8sException( 500, e.getMessage() );
        }
    }

    @Post("/xp7/{namespace}/{name}/{nodegroup}/mgmt/apps/stop")
    @Consumes("application/json")
    public void appStop( @PathVariable("namespace") final String namespace, @PathVariable("name") final String name,
                         @PathVariable("nodegroup") final String nodeGroup, @Body final AppKey key )
        throws KubernetesClientException
    {
        try
        {
            getClient( namespace, name, nodeGroup ).appStop( key );
        }
        catch ( Exception e )
        {
            logger.error( "Failed stopping app", e );
            throw k8sException( 500, e.getMessage() );
        }
    }
}
