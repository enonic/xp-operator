package com.enonic.kubernetes.apis.xp.service;

public enum AppEventType
{
    LIST, INSTALLED, STATE, UNINSTALLED;

    public static AppEventType fromEventName( final String name )
    {
        switch ( name )
        {
            case "list":
                return AppEventType.LIST;
            case "installed":
                return AppEventType.INSTALLED;
            case "state":
                return AppEventType.STATE;
            case "uninstalled":
                return AppEventType.UNINSTALLED;
            default:
                throw new RuntimeException( String.format( "Unknown SSE event '%s'", name ) );
        }
    }
}