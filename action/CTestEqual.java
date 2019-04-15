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

import com.codepoetics.protonpack.StreamUtils;
import com.google.common.collect.Multimap;
import org.lightjason.agentspeak.action.IBaseAction;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * test action for equality
 */
public final class CTestEqual extends IBaseAction
{
    /**
     * serial id
     */
    private static final long serialVersionUID = -2411595374105128516L;
    /**
     * action name
     */
    private static final IPath NAME = CPath.of( "test/equal" );

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
    public final Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                                 @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
    {
        StreamUtils.windowed(
            p_argument.stream(),
            2,
            2
        ).map( i -> CCommon.streamconcatstrict(
            equalcollection( i.get( 0 ), i.get( 1 ) ),
            equalmap( i.get( 0 ), i.get( 1 ) ),
            equalmultimap( i.get( 0 ), i.get( 1 ) ),
            equalobject( i.get( 0 ), i.get( 1 ) ) ).filter( j -> j ).findFirst().orElse( false )
        ).map( CRawTerm::of ).forEach( p_return::add );

        return Stream.empty();
    }

    /**
     * compare any objects
     *
     * @param p_source source term
     * @param p_target target term
     * @return equality boolean flag as stream
     */
    private static Stream<Boolean> equalobject( @Nonnull final ITerm p_source, @Nonnull final ITerm p_target )
    {
        return Objects.isNull( p_source.raw() ) && Objects.isNull( p_target.raw() )
               ? Stream.of( true )
               : Objects.isNull( p_source.raw() ) ^ Objects.isNull( p_target.raw() )
                 ? Stream.of( false )
                 : Stream.of( p_source.raw().equals( p_target.raw() ) );
    }


    /**
     * compares collections
     *
     * @param p_source source term
     * @param p_target target term
     * @return equality boolean flag as stream
     */
    private static Stream<Boolean> equalcollection( @Nonnull final ITerm p_source, @Nonnull final ITerm p_target )
    {
        return CCommon.isssignableto( p_source, Collection.class ) && CCommon.isssignableto( p_target, Collection.class )
               ? Stream.of( Arrays.equals( p_source.<Collection<?>>raw().toArray(), p_target.<Collection<?>>raw().toArray() ) )
               : Stream.empty();
    }


    /**
     * compare maps
     *
     * @param p_source source object
     * @param p_target object to compare
     * @return equality boolean flag as stream
     */
    private static Stream<Boolean> equalmap( @Nonnull final ITerm p_source, @Nonnull final ITerm p_target )
    {
        return CCommon.isssignableto( p_source, Map.class ) && CCommon.isssignableto( p_target, Map.class )
               ? Stream.of( Arrays.equals( p_source.<Map<?, ?>>raw().keySet().toArray(), p_target.<Map<?, ?>>raw().keySet().toArray() )
                            && Arrays.equals( p_source.<Map<?, ?>>raw().values().toArray(), p_target.<Map<?, ?>>raw().values().toArray() ) )
               : Stream.empty();
    }


    /**
     * compare multimap
     *
     * @param p_source source object
     * @param p_target object to compare
     * @return equality boolean flag as stream
     */
    private static Stream<Boolean> equalmultimap( @Nonnull final ITerm p_source, @Nonnull final ITerm p_target )
    {
        return CCommon.isssignableto( p_source, Multimap.class ) && CCommon.isssignableto( p_target, Multimap.class )
               ? Stream.of( Arrays.equals( p_source.<Multimap<?, ?>>raw().keySet().toArray(), p_target.<Multimap<?, ?>>raw().keySet().toArray() )
                            && Arrays.equals( p_source.<Multimap<?, ?>>raw().values().toArray(), p_target.<Multimap<?, ?>>raw().values().toArray() ) )
               : Stream.empty();
    }
}
