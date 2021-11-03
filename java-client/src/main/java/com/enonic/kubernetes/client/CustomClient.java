package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.v1.Clients;

public interface CustomClient {
    Clients v1();
}
