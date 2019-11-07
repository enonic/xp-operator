package com.enonic.ec.kubernetes.crd.vhost.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;

@Value.Immutable
public abstract class DiffSpec
    extends Diff<Spec>
{

}
