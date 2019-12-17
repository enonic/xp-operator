package com.enonic.ec.kubernetes.operator.common.commands;

public interface Command<T>
{
    T execute()
        throws Exception;
}
