package com.enonic.kubernetes.operator.xp7app;

import com.enonic.kubernetes.client.v1.xp7app.Xp7App;

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
            app.getStatus().getXp7AppStatusFields() != null &&
            app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() != null;
    }

    static Predicate<Xp7App> matchesKey( final String key )
    {
        return successfullyInstalled().
            and( app -> app.
                getStatus().
                getXp7AppStatusFields().
                getXp7AppStatusFieldsAppInfo().
                getKey().
                equals( key ) );
    }
}
