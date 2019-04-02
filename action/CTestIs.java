/*
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason                                                #
 * # Copyright (c) 2015-19, LightJason (info@lightjason.org)                            #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package org.lightjason.agentspeak.testing.action;

import org.lightjason.agentspeak.action.IBaseAction;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.error.context.CExecutionException;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * test is action
 */
public final class CTestIs extends IBaseAction
{
    /**
     * serial id
     */
    private static final long serialVersionUID = 1479225623609385322L;
    /**
     * action name
     */
    private static final IPath NAME = CPath.of( "test/is" );

    @Nonnull
    @Override
    public IPath name()
    {
        return NAME;
    }

    @Override
    public int minimalArgumentNumber()
    {
        return 2;
    }

    @Nonnull
    @Override
    public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context, @Nonnull final List<ITerm> p_argument,
                                           @Nonnull final List<ITerm> p_return )
    {
        final List<ITerm> l_arguments = org.lightjason.agentspeak.language.CCommon.flatten( p_argument ).collect( Collectors.toList() );
        if ( "null".equalsIgnoreCase( l_arguments.get( 0 ).raw() ) )
            return l_arguments.stream()
                              .skip( 1 )
                              .map( ITerm::raw )
                              .allMatch( Objects::isNull )
                   ? p_context.agent().fuzzy().membership().success()
                   : p_context.agent().fuzzy().membership().fail();

        final Class<?> l_class;
        try
        {
            l_class = Class.forName( l_arguments.get( 0 ).raw() );
        }
        catch ( final ClassNotFoundException l_exception )
        {
            throw new CExecutionException( p_context, l_exception );
        }

        return l_arguments.stream()
                          .skip( 1 )
                          .map( ITerm::raw )
                          .allMatch( i -> l_class.isAssignableFrom( i.getClass() ) )
               ? p_context.agent().fuzzy().membership().success()
               : p_context.agent().fuzzy().membership().fail();
    }
}
