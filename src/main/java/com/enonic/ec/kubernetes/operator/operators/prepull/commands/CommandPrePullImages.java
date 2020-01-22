package com.enonic.ec.kubernetes.operator.operators.prepull.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyDaemonSet;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

@Value.Immutable
public abstract class CommandPrePullImages
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract List<String> versions();

    private String getOperatorNamespace()
    {
        try
        {
            //noinspection SpellCheckingInspection
            return new String( Files.readAllBytes( Paths.get( "/var/run/secrets/kubernetes.io/serviceaccount/namespace" ) ) );
        }
        catch ( IOException e )
        {
            return "ec-system";
        }
    }

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        if ( versions().size() < 1 )
        {
            return;
        }

        String name = "ec-pre-pull-images";

        String imageTemplate = cfgStr( "operator.deployment.xp.pod.imageTemplate" );
        List<String> commands = versions().stream().
            map( v -> String.format( imageTemplate, v ) ).
            map( v -> "docker pull " + v ).
            collect( Collectors.toList() );

        String mountPath = "/var/run";
        String mountName = "docker";

        Map<String, String> labels = Map.of( "app", name );

        DaemonSetSpec spec = new DaemonSetSpec();
        spec.setSelector( new LabelSelector( null, labels ) );

        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
        spec.setTemplate( podTemplateSpec );

        ObjectMeta podMeta = new ObjectMeta();
        podMeta.setLabels( labels );
        podTemplateSpec.setMetadata( podMeta );

        PodSpec podSpec = new PodSpec();
        podTemplateSpec.setSpec( podSpec );

        Container prePull = new Container();
        podSpec.setInitContainers( Collections.singletonList( prePull ) );

        prePull.setName( "pre-pull" );
        prePull.setImage( "docker" );
        prePull.setCommand( Arrays.asList( "ash", "-c", String.join( " && ", commands ) ) );
        prePull.setVolumeMounts( Collections.singletonList( new VolumeMount( mountPath, null, mountName, null, null, null ) ) );

        Container pause = new Container();
        pause.setName( "pause" );
        pause.setImage( "gcr.io/google_containers/pause" );
        podSpec.setContainers( Collections.singletonList( pause ) );

        Volume vol = new Volume();
        podSpec.setVolumes( Collections.singletonList( vol ) );
        vol.setName( mountName );
        vol.setHostPath( new HostPathVolumeSource( mountPath, null ) );

        commandBuilder.addCommand( ImmutableCommandApplyDaemonSet.builder().
            clients( clients() ).
            namespace( getOperatorNamespace() ).
            name( name ).
            canSkipOwnerReference( true ).
            spec( spec ).
            build() );
    }
}
