package com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.vhost;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public enum V1alpha1Xp7VHostSpecCertificateAuthority
{
    @JsonProperty("selfSigned") SELF_SIGNED, @JsonProperty("letsEncrypt") LETS_ENCRYPT_PROD, @JsonProperty("letsEncryptStaging") LETS_ENCRYPT_STAGING
}
