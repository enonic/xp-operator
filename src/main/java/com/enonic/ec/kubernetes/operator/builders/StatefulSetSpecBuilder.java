package com.enonic.ec.kubernetes.operator.builders;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;

import com.enonic.ec.kubernetes.common.Builder;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class StatefulSetSpecBuilder
    implements Builder<StatefulSetSpec>
{
    private Map<String, String> podLabels;

    private String serviceName;

    private Integer replicas;

    private StatefulSetSpecBuilder()
    {
    }

    public static StatefulSetSpecBuilder newBuilder()
    {
        return new StatefulSetSpecBuilder();
    }

    public StatefulSetSpecBuilder podLabels( final Map<String, String> val )
    {
        podLabels = val;
        return this;
    }

    public StatefulSetSpecBuilder serviceName( final String val )
    {
        serviceName = val;
        return this;
    }

    public StatefulSetSpecBuilder replicas( final Integer val )
    {
        replicas = val;
        return this;
    }

    @Override
    public StatefulSetSpec build()
    {
        podLabels = assertNotNull( "podLabels", podLabels );
        serviceName = assertNotNull( "serviceName", serviceName );
        replicas = assertNotNull( "replicas", replicas );

        StatefulSetSpec spec = new StatefulSetSpec();

        spec.setSelector( new LabelSelector( null, podLabels ) );
        spec.setServiceName( serviceName );
        spec.setReplicas( replicas );
        spec.setPodManagementPolicy( "Parallel" );
        spec.setUpdateStrategy( new StatefulSetUpdateStrategy( null, "RollingUpdate" ) );
        spec.setTemplate( createPodSpec() );
        spec.setVolumeClaimTemplates( createVolumeClaimTemplates() );

        return spec;
    }

    private PodTemplateSpec createPodSpec()
    {
        return null; // TODO
    }

    private List<PersistentVolumeClaim> createVolumeClaimTemplates()
    {
        return null; // TODO
    }
}
