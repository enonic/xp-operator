package com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Xp7VHostSpecCertificateAuthority
{
    @JsonProperty("selfSigned") SELF_SIGNED, @JsonProperty("letsEncrypt") LETS_ENCRYPT_PROD, @JsonProperty("letsEncryptStaging") LETS_ENCRYPT_STAGING
}
