package com.enonic.cloud.operator.v1alpha2xp7config;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import okhttp3.Response;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.ResourceQuery;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7ConfigStatus;
import com.enonic.cloud.operator.helpers.HandlerStatus;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class OperatorXp7ConfigStatus
    extends HandlerStatus<Xp7Config, Xp7ConfigStatus>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7ConfigStatus.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    String code = "";

    @Override
    protected InformerSearcher<Xp7Config> informerSearcher()
    {
        return searchers.xp7Config();
    }

    @Override
    protected Xp7ConfigStatus getStatus( final Xp7Config resource )
    {
        return resource.getXp7ConfigStatus();
    }

    @Override
    protected Doneable<Xp7Config> updateStatus( final Xp7Config resource, final Xp7ConfigStatus newStatus )
    {
        return clients.xp7Configs().crdClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withStatus( newStatus );
    }

    @Override
    protected Xp7ConfigStatus pollStatus( final Xp7Config config )
    {
        // If config has been flagged ready, do not try to set the state again
        if ( config.getXp7ConfigStatus().getState() == Xp7ConfigStatus.State.READY )
        {
            return config.getXp7ConfigStatus();
        }

        ResourceQuery<Pod> stream = searchers.pod().query().
            inNamespace( config.getMetadata().getNamespace() );

        if ( !Objects.equals( config.getXp7ConfigSpec().getNodeGroup(), cfgStr( "operator.helm.charts.Values.allNodesKey" ) ) )
        {
            stream =
                stream.hasLabel( cfgStr( "operator.helm.charts.Values.labelKeys.nodeGroup" ), config.getXp7ConfigSpec().getNodeGroup() );
        }

        String fileName = config.getXp7ConfigSpec().getFile();
        String expectedContents = config.getXp7ConfigSpec().getData();
        if ( !config.getXp7ConfigSpec().getDataBase64() )
        {
            expectedContents = BaseEncoding.
                base64().
                encode( expectedContents.getBytes() );
        }

        for ( Pod p : stream.list() )
        {
            if ( !configLoaded( p, fileName, expectedContents ) )
            {
                return new Xp7ConfigStatus().
                    withState( Xp7ConfigStatus.State.PENDING ).
                    withMessage( "Not loaded: " + p.getMetadata().getName() );
            }
        }

        return new Xp7ConfigStatus().
            withState( Xp7ConfigStatus.State.READY ).
            withMessage( "OK" );
    }

    private boolean configLoaded( final Pod pod, final String fileName, final String expectedContents )
    {
        if ( !Objects.equals( pod.getStatus().getPhase(), "Running" ) )
        {
            return false;
        }

        String base64Contents = null;
        try
        {
            final boolean[] done = {false};
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ExecWatch exec = clients.k8s().pods().
                inNamespace( pod.getMetadata().getNamespace() ).
                withName( pod.getMetadata().getName() ).
                writingOutput( os ).
                usingListener( new ExecListener()
                {
                    @Override
                    public void onOpen( final Response response )
                    {
                        // Do Nothing
                    }

                    @Override
                    public void onFailure( final Throwable throwable, final Response response )
                    {
                        // Do Nothing
                    }

                    @Override
                    public void onClose( final int i, final String s )
                    {
                        done[0] = true;
                    }
                } ).
                exec( "base64", "-w", "0", String.format( "%s/%s", cfgStr( "operator.helm.charts.Values.configLocation" ), fileName ) );
            while ( !done[0] )
            {
                Thread.sleep( 200L );
            }
            exec.close();
            base64Contents = IOUtils.toString( os.toByteArray(), "UTF-8" );
        }
        catch ( Exception e )
        {
            log.debug( "Failed getting config from pod", e );
        }

        return Objects.equals( expectedContents, base64Contents );
    }
}
