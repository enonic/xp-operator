package com.enonic.cloud.operator.api;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.kubernetes.InformerSearcher;

public class TestInformerSearcher<T extends HasMetadata>
    extends InformerSearcher<T>
{
    private final List<T> list;

    public TestInformerSearcher()
    {
        this.list = new LinkedList<>();
    }

    @Override
    public Stream<T> stream()
    {
        return list.stream();
    }

    public void add( T r )
    {
        list.add( r );
    }
}
