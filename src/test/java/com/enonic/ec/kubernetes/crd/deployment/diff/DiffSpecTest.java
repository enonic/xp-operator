package com.enonic.ec.kubernetes.crd.deployment.diff;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.enonic.ec.kubernetes.DeploymentFileTest;

class DiffSpecTest
    extends DeploymentFileTest
{
    private DiffSpec loadDiff( String oldFile, String newFile )
    {
        return ImmutableDiffSpec.builder().
            oldValue( oldFile == null ? Optional.empty() : Optional.of( loadResource( oldFile ).getSpec() ) ).
            newValue( loadResource( newFile ).getSpec() ).
            build();
    }

    private DiffSpec loadDiff( String newFile )
    {
        return loadDiff( null, newFile );
    }

    @Test
    public void newDeployment()
    {
        loadDiff( "planFull_old.yaml" );
    }

}