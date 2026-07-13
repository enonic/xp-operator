package com.enonic.kubernetes.operator.xp7app;

import com.enonic.kubernetes.crd.v1.Xp7App;

import java.util.function.Predicate;

public class Predicates
{
    static Predicate<Xp7App> notSuccessfullyInstalled()
    {
        return successfullyInstalled().negate();
    }

    static Predicate<Xp7App> successfullyInstalled()
    {
        return ( app ) -> app.getStatus() != null &&
            app.getStatus().getFields() != null &&
            app.getStatus().getFields().getAppInfo() != null;
    }

    static Predicate<Xp7App> matchesKey( final String key )
    {
        return successfullyInstalled().
            and( app -> app.
                getStatus().
                getFields().
                getAppInfo().
                getKey().
                equals( key ) );
    }
}
