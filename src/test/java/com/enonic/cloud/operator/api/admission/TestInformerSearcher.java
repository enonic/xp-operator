package com.enonic.cloud.operator.api.admission;

import java.util.LinkedList;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.ResourceQuery;

public class TestInformerSearcher<T extends HasMetadata>
    extends InformerSearcher<T>
{
    private final List<T> list;

    public TestInformerSearcher()
    {
        this.list = new LinkedList<>();
    }

    @Override
    public ResourceQuery<T> query()
    {
        return new ResourceQuery<>( list.stream() );
    }

    public void add( T r )
    {
        list.add( r );
    }
}
