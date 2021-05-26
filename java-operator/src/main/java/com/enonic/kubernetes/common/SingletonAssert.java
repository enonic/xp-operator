package com.enonic.kubernetes.common;

import java.util.HashSet;
import java.util.Set;

import static com.enonic.kubernetes.common.Exit.exit;

public class SingletonAssert
{
    private static Set<String> set = new HashSet<>();

    public static void singletonAssert( Object obj, String key )
    {
        singletonAssert( obj.getClass(), key );
    }

    public static void singletonAssert( Class klass, String key )
    {
        String k = klass.getSimpleName() + ":" + key;
        synchronized (set) {
            if (set.contains( k )) {
                exit( Exit.Code.SINGLETON_FAILED, String.format( "Singleton %s called multiple times", k ) );
            }
            set.add( k );
        }
    }
}
