package com.enonic.kubernetes.operator.ingress;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MappingTest
{
    @Test
    void parseContextKeepsOrder()
    {
        assertThat( Mapping.parseContext( "b=2\r\n\n a = 1 \nc=x=y" ) ).containsExactly( Map.entry( "b", "2" ), Map.entry( "a", "1" ),
                                                                                        Map.entry( "c", "x=y" ) );
    }

    @Test
    void parseContextOfNothing()
    {
        assertThat( Mapping.parseContext( null ) ).isEmpty();
        assertThat( Mapping.parseContext( "\n  \n" ) ).isEmpty();
    }

    @Test
    void parseContextAllowsEmptyValue()
    {
        assertThat( Mapping.parseContext( "a=" ) ).containsExactly( Map.entry( "a", "" ) );
    }

    @Test
    void parseContextRejectsLineWithoutEquals()
    {
        assertThatThrownBy( () -> Mapping.parseContext( "a=1\nb" ) ).isInstanceOf( IllegalArgumentException.class )
            .hasMessage( "context line 'b' has no '='" );
    }

    @Test
    void parseContextRejectsInvalidKeys()
    {
        for ( String line : new String[]{"=1", "a b=1", "#a=1", "!a=1", "a\\b=1", "\\a=1"} )
        {
            assertThatThrownBy( () -> Mapping.parseContext( line ) ).isInstanceOf( IllegalArgumentException.class )
                .hasMessageEndingWith( "is not a valid configuration key" );
        }
    }

    @Test
    void parseContextRejectsBackslashInValue()
    {
        assertThatThrownBy( () -> Mapping.parseContext( "a=b\\" ) ).isInstanceOf( IllegalArgumentException.class )
            .hasMessage( "context value of 'a' contains a backslash" );
    }

    @Test
    void parseContextRejectsRepeatedKey()
    {
        assertThatThrownBy( () -> Mapping.parseContext( "a=1\na=2" ) ).isInstanceOf( IllegalArgumentException.class )
            .hasMessage( "context key 'a' is given more than once" );
    }
}
