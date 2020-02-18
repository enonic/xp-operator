package com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum V1alpha2Xp7VHostSpecCertificateAuthority
{
    @JsonProperty("selfSigned") SELF_SIGNED, @JsonProperty("letsEncrypt") LETS_ENCRYPT_PROD, @JsonProperty("letsEncryptStaging") LETS_ENCRYPT_STAGING
}
