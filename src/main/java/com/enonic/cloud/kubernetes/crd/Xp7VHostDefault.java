package com.enonic.cloud.kubernetes.crd;

import java.util.Collections;

import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMappingOptions;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecOptions;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatusFields;

public class Xp7VHostDefault
{
    public static Xp7VHost addDefaultValues( final Xp7VHost in )
    {
        if ( in.getXp7VHostSpec().getXp7VHostSpecOptions() == null )
        {
            in.getXp7VHostSpec().withXp7VHostSpecOptions( new Xp7VHostSpecOptions().
                withCdn( true ).
                withDnsRecord( true ) );
        }

        for ( Xp7VHostSpecMapping m : in.getXp7VHostSpec().getXp7VHostSpecMappings() )
        {
            if ( m.getXp7VHostSpecMappingOptions() == null )
            {
                m.withXp7VHostSpecMappingOptions( new Xp7VHostSpecMappingOptions().
                    withIngress( true ).
                    withIngressMaxBodySize( "100m" ).
                    withStatusCake( false ).
                    withIpWhitelist( "" ).
                    withStickySession( false ).
                    withSslRedirect( in.getXp7VHostSpec().getXp7VHostSpecCertificate() != null ) );
            }
        }

        if ( in.getXp7VHostStatus() == null )
        {
            in.withXp7VHostStatus( new Xp7VHostStatus().
                withState( Xp7VHostStatus.State.PENDING ).
                withMessage( "Created" ).
                withXp7VHostStatusFields( new Xp7VHostStatusFields().
                    withDnsRecordCreated( false ).
                    withPublicIps( Collections.emptyList() ) ) );
        }

        return in;
    }
}
