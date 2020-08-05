package com.enonic.cloud.operator.v1alpha1xp7app;

import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFieldsAppInfo;

public class AppStatusBuilder
{
    private Xp7AppStatus appStatus;

    public AppStatusBuilder()
    {
        this.appStatus = new Xp7AppStatus();
    }

    public AppStatusBuilder( Xp7AppStatus appStatus )
    {
        this();
        this.withState( appStatus.getState() );
        this.withMessage( appStatus.getMessage() );
        this.withFields( appStatus.getXp7AppStatusFields() );
    }

    public AppStatusBuilder withState( Xp7AppStatus.State state )
    {
        this.appStatus.setState( state );
        return this;
    }

    public AppStatusBuilder withMessage( String message )
    {
        this.appStatus.setMessage( message );
        return this;
    }

    public AppStatusBuilder withFields( Xp7AppStatusFields fields )
    {
        this.appStatus.setXp7AppStatusFields( fields );
        return this;
    }

    public AppStatusBuilder withAppInfo( AppInfo appInfo )
    {
        if ( appStatus.getXp7AppStatusFields() == null )
        {
            this.withFields( new Xp7AppStatusFields() );
        }

        if ( appInfo != null )
        {
            appStatus.getXp7AppStatusFields().
                withXp7AppStatusFieldsAppInfo( new Xp7AppStatusFieldsAppInfo().
                    withDescription( appInfo.description() ).
                    withDisplayName( appInfo.displayName() ).
                    withKey( appInfo.key() ).
                    withModifiedTime( appInfo.modifiedTime() ).
                    withState( appInfo.state() ).
                    withUrl( appInfo.url() ).
                    withVendorName( appInfo.vendorName() ).
                    withUrl( appInfo.vendorUrl() ).
                    withVersion( appInfo.version() ) );
        }
        else
        {
            appStatus.getXp7AppStatusFields().setXp7AppStatusFieldsAppInfo( null );
        }

        return this;
    }

    public Xp7AppStatus build()
    {
        return appStatus;
    }
}
