package com.enonic.cloud.helm.functions;

import java.util.List;
import java.util.function.Function;

import javax.inject.Singleton;

import com.google.common.base.Preconditions;

import com.enonic.cloud.kubernetes.commands.K8sCommand;
import com.enonic.cloud.kubernetes.commands.K8sCommandAction;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class K8sCommandSorter
    implements Function<List<K8sCommand>, List<K8sCommand>>
{
    @Override
    public List<K8sCommand> apply( List<K8sCommand> o )
    {
        o.sort( this::sort );
        return o;
    }

    @SuppressWarnings("DuplicatedCode")
    private int sort( final K8sCommand a, final K8sCommand b )
    {
        if ( a.action() != b.action() )
        {
            return sortActions( a.action(), b.action() );
        }

        String priorityAnnotation = cfgStr( "operator.charts.values.annotationKeys.applyPriority" );

        Preconditions.checkState( a.resource().getMetadata().getAnnotations() != null,
                                  String.format( "Annotations cannot be null on '%s'", a.resource().getMetadata().getName() ) );
        String ap = a.resource().getMetadata().getAnnotations().get( priorityAnnotation );
        Preconditions.checkState( ap != null, String.format( "Annotation '%s' cannot be null on '%s'", priorityAnnotation,
                                                             a.resource().getMetadata().getName() ) );

        Preconditions.checkState( b.resource().getMetadata().getAnnotations() != null,
                                  String.format( "Annotations cannot be null on '%s'", b.resource().getMetadata().getName() ) );
        String bp = b.resource().getMetadata().getAnnotations().get( priorityAnnotation );
        Preconditions.checkState( bp != null, String.format( "Annotation '%s' cannot be null on '%s'", priorityAnnotation,
                                                             b.resource().getMetadata().getName() ) );

        int api = Integer.parseInt( ap );
        int bpi = Integer.parseInt( bp );

        int multiplier = a.action() == K8sCommandAction.DELETE ? -1 : 1;

        if ( api > bpi )
        {
            //noinspection PointlessArithmeticExpression
            return 1 * multiplier;
        }
        else if ( api == bpi )
        {
            return 0;
        }
        else
        {
            return -1 * multiplier;
        }
    }

    private int sortActions( final K8sCommandAction a, final K8sCommandAction b )
    {
        if ( a == K8sCommandAction.CREATE )
        {
            return 1;
        }
        else if ( a == K8sCommandAction.DELETE )
        {
            return -1;
        }

        // A is MODIFY / OVERWRITE
        if ( b == K8sCommandAction.CREATE )
        {
            return -1;
        }
        else if ( b == K8sCommandAction.DELETE )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }
}
