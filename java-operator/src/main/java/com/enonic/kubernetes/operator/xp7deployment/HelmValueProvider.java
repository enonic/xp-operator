package com.enonic.kubernetes.operator.xp7deployment;

import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ServiceAccount;

import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.operator.helpers.PasswordGenerator;

import static com.enonic.kubernetes.common.Configuration.cfgHasKey;
import static com.enonic.kubernetes.common.Configuration.cfgStr;

@Singleton
public class HelmValueProvider
{
    private static final Logger log = LoggerFactory.getLogger( HelmValueProvider.class );

    @Produces
    @Named("suPass")
    Supplier<String> suPassSupplier()
    {
        if ( cfgHasKey( "operator.deployment.fixedSuPass" ) )
        {
            log.warn( "Password provider fixed to a single password" );
            return () -> cfgStr( "operator.deployment.fixedSuPass" );
        }
        else
        {
            return () -> PasswordGenerator.getPassword( 32 );
        }
    }

    @Produces
    @Named("cloudApi")
    Supplier<ServiceAccount> saSupplier( Clients clients )
    {
        if ( cfgHasKey( "operator.cloudApi.name" ) && cfgHasKey( "operator.cloudApi.namespace" ) )
        {
            final ServiceAccount realSa = clients.k8s().serviceAccounts().
                inNamespace( cfgStr( "operator.cloudApi.namespace" ) ).
                withName( cfgStr( "operator.cloudApi.name" ) ).
                get();

            if ( realSa == null )
            {
                log.warn( "Cloud API SA not found, permissions for it will not be created" );
            }

            return () -> realSa;
        }

        return () -> null;
    }
}
