package com.enonic.kubernetes.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PreconditionsTest
{
    @Test
    void aCheckThatHoldsDoesNotThrow()
    {
        assertDoesNotThrow( () -> Preconditions.checkState( true, "message" ) );
        assertDoesNotThrow( () -> Preconditions.checkArgument( true, "message" ) );
    }

    @Test
    void checkStateFailsWithTheFormattedMessage()
    {
        assertThatThrownBy( () -> Preconditions.checkState( false, "No Xp8Deployment found in NS '%s'", "my-namespace" ) )
            .isInstanceOf( IllegalStateException.class )
            .hasMessage( "No Xp8Deployment found in NS 'my-namespace'" );
    }

    @Test
    void checkArgumentFailsWithTheFormattedMessage()
    {
        assertThatThrownBy( () -> Preconditions.checkArgument( false, "port %s is not served", 4848 ) )
            .isInstanceOf( IllegalArgumentException.class )
            .hasMessage( "port 4848 is not served" );
    }

    @Test
    void aMessageWithoutArgumentsIsNotReadAsAFormat()
    {
        assertThatThrownBy( () -> Preconditions.checkState( false, "cpu must be below 100%" ) )
            .isInstanceOf( IllegalStateException.class )
            .hasMessage( "cpu must be below 100%" );
    }
}
