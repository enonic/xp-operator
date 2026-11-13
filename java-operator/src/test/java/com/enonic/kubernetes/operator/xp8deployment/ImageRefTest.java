package com.enonic.kubernetes.operator.xp8deployment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageRefTest
{
    private static final String PREFIX = "proxy.io/hub";

    @Test
    void withoutAPrefixTheImageIsUnchanged()
    {
        assertThat( ImageRef.resolve( "busybox", "" ) ).isEqualTo( "busybox" );
    }

    @Test
    void aDockerHubOfficialImageGetsTheLibraryPath()
    {
        assertThat( ImageRef.resolve( "busybox", PREFIX ) ).isEqualTo( "proxy.io/hub/library/busybox" );
    }

    @Test
    void aTrailingSlashOfThePrefixIsIgnored()
    {
        assertThat( ImageRef.resolve( "busybox", PREFIX + "/" ) ).isEqualTo( "proxy.io/hub/library/busybox" );
    }

    @Test
    void anImageWithAnOwnerIsPrefixed()
    {
        assertThat( ImageRef.resolve( "enonic/xp:8.2.0-ubuntu", PREFIX ) ).isEqualTo( "proxy.io/hub/enonic/xp:8.2.0-ubuntu" );
    }

    @Test
    void anImageThatNamesItsRegistryIsUnchanged()
    {
        assertThat( ImageRef.resolve( "ghcr.io/enonic/xp:8.2.0", PREFIX ) ).isEqualTo( "ghcr.io/enonic/xp:8.2.0" );
        assertThat( ImageRef.resolve( "localhost/xp", PREFIX ) ).isEqualTo( "localhost/xp" );
        assertThat( ImageRef.resolve( "registry:5000/xp", PREFIX ) ).isEqualTo( "registry:5000/xp" );
    }
}
