package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.idproviders.Xp7MgmtIdProvider;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

import java.util.List;

public class Xp7MgmtApiIdProvidersImpl
    extends Xp7MgmtClient
    implements Xp7MgmtApiIdProviders
{
    public Xp7MgmtApiIdProvidersImpl( HttpClient client, Config config, String namespace, String name, String nodeGroup )
    {
        super( client, config, namespace, name, nodeGroup );
    }

    public List<Xp7MgmtIdProvider> list()
    {
        return requestList( requestUrl( "idproviders/list" ), Xp7MgmtIdProvider.class );
    }
}
