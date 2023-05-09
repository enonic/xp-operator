package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.webapps.Xp7MgmtWebapp;

import java.util.List;

public interface Xp7MgmtApiWebapps {
    List<Xp7MgmtWebapp> list();
}
