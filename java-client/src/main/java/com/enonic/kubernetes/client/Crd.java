package com.enonic.kubernetes.client;

import io.fabric8.kubernetes.client.CustomResource;

import java.util.Objects;

public class Crd<S, T>
    extends CustomResource<S, T>
{
    @Override
    public int hashCode()
    {
        int result = 1;
        result = ((result * 31) + ((this.getApiVersion() == null) ? 0 : this.getApiVersion().hashCode()));
        result = ((result * 31) + ((this.getKind() == null) ? 0 : this.getKind().hashCode()));
        result = ((result * 31) + ((this.getMetadata() == null) ? 0 : this.getMetadata().hashCode()));
        result = ((result * 31) + ((this.getSpec() == null) ? 0 : this.getSpec().hashCode()));
        result = ((result * 31) + ((this.getStatus() == null) ? 0 : this.getStatus().hashCode()));
        return result;
    }

    @Override
    public boolean equals( Object other )
    {
        if (other == null) {
            return false;
        }

        if (other == this) {
            return true;
        }

        if (!other.getClass().equals( this.getClass() )) {
            return false;
        }

        Crd rhs = ((Crd) other);
        return Objects.equals( rhs.getApiVersion(), this.getApiVersion() ) &&
            Objects.equals( rhs.getKind(), this.getKind() ) &&
            Objects.equals( rhs.getMetadata(), this.getMetadata() ) &&
            Objects.equals( rhs.getSpec(), this.getSpec() ) &&
            Objects.equals( rhs.getStatus(), this.getStatus() );
    }
}
