package com.enonic.kubernetes.operator.xp8deployment;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class ImageRef
{
    private ImageRef()
    {
    }

    static String resolve( final String image, final String registryPrefix )
    {
        if ( registryPrefix.isEmpty() )
        {
            return image;
        }
        final String[] parts = image.split( "/", -1 );
        final String first = parts[0];
        final boolean namesRegistry = parts.length > 1 && ( first.contains( "." ) || first.contains( ":" ) || first.equals( "localhost" ) );
        if ( namesRegistry )
        {
            return image;
        }
        final String prefix = registryPrefix.endsWith( "/" ) ? registryPrefix.substring( 0, registryPrefix.length() - 1 ) : registryPrefix;
        final boolean dockerHubOfficialImage = parts.length == 1;
        return dockerHubOfficialImage ? prefix + "/library/" + image : prefix + "/" + image;
    }
}
