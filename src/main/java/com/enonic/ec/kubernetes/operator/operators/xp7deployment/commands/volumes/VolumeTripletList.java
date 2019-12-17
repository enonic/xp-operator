package com.enonic.ec.kubernetes.operator.operators.xp7deployment.commands.volumes;

import java.util.LinkedList;
import java.util.List;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;

public class VolumeTripletList
    extends LinkedList<VolumeTriplet>
{
    public List<VolumeMount> volumeMounts()
    {
        List<VolumeMount> res = new LinkedList<>();
        this.forEach( t -> res.addAll( t.volumeMount() ) );
        return res;
    }

    public List<Volume> volumes()
    {
        List<Volume> res = new LinkedList<>();
        this.forEach( t -> t.volume().ifPresent( res::add ) );
        return res;
    }

    public List<PersistentVolumeClaim> volumeClaimTemplates()
    {
        List<PersistentVolumeClaim> res = new LinkedList<>();
        this.forEach( t -> t.volumeClaim().ifPresent( res::add ) );
        return res;
    }
}
