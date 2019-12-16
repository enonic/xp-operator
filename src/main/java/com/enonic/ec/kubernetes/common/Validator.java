package com.enonic.ec.kubernetes.common;

import com.google.common.base.Preconditions;

public class Validator
{
    public static void dnsName( String name, String value )
    {
        Preconditions.checkState( value.matches( "^[a-z]([-a-z0-9]*[a-z0-9])?$" ), "Field '" + name + "' with value '" + value +
            "' is not valid, it must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name',  or 'abc-123', regex used for validation is '[a-z]([-a-z0-9]*[a-z0-9])?')" );
    }
}
