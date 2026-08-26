package com.enonic.kubernetes.common;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.enonic.kubernetes.common.Exit.exit;

public class SingletonAssert
{
    private static final Set<String> set = ConcurrentHashMap.newKeySet();

    public static void singletonAssert( Object obj, String key )
    {
        singletonAssert( obj.getClass(), key );
    }

    public static void singletonAssert( Class klass, String key )
    {
        String k = klass.getSimpleName() + ":" + key;
        if (set.contains( k )) {
            exit( Exit.Code.SINGLETON_FAILED, String.format( "Singleton %s called multiple times", k ) );
        }
        set.add( k );
    }
}
