package com.enonic.ec.kubernetes.common.commands;

public interface Command<T>
{
    T execute()
        throws Exception;
}
