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

package org.lightjason.agentspeak.testing;

import com.google.common.collect.Multimap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lightjason.agentspeak.action.IBaseAction;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.common.CCommon;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.error.context.CExecutionException;
import org.lightjason.agentspeak.generator.CActionStaticGenerator;
import org.lightjason.agentspeak.generator.CLambdaStreamingStaticGenerator;
import org.lightjason.agentspeak.grammar.builder.CRaw;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.language.variable.CConstant;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * test agent structure.
 * If a file agentprintin.conf exists on the main directory alls print statements will be shown
 */
@RunWith( DataProviderRunner.class )
public final class TestCAsl extends IBaseTest
{
    /**
     * tag of iteration
     */
    private static final String ITERATIONNTAG = "@iteration";
    /**
     * regex for iteration
     */
    private static final Pattern ITERATION = Pattern.compile( ITERATIONNTAG + "\\s+\\d+" );
    /**
     * tag of test results
     */
    private static final String TESTCOUNTTAG = "@testcount";
    /**
     * regex for iteration
     */
    private static final Pattern TESTCOUNT = Pattern.compile( TESTCOUNTTAG + "\\s+\\d+" );
    /**
     * iteration counter
     */
    private AtomicInteger m_count;

    static
    {
        // disable logger
        LogManager.getLogManager().reset();
    }

    /**
     * initialize
     */
    @Before
    public void initialize()
    {
        m_count = new AtomicInteger();
    }

    /**
     * data provider for defining asl files
     * @return triple of test-cases (asl file, number of iterations, expected log items)
     */
    @DataProvider
    public static Object[] generate()
    {
        try
        (
            final Stream<Path> l_walk = Files.walk(
                Paths.get( TestCAsl.class.getClassLoader().getResource( "" ).getPath(), "asl" )
            )
        )
        {
            return l_walk.filter( Files::isRegularFile )
                         .map( Path::toString )
                         .filter( i -> i.endsWith( ".asl" ) )
                         .toArray();

        }
        catch ( final IOException l_exception )
        {
            l_exception.printStackTrace();
            Assert.fail();
            return Stream.of().toArray();
        }
    }


    /**
     * test for default generators and configuration
     *
     * @param p_file tripel of asl code, cycles and expected success calls
     * @throws Exception on any error
     */
    @Test
    @UseDataProvider( "generate" )
    public void testASLDefault( @Nonnull final String p_file ) throws Exception
    {
        final IAgent<?> l_agent;
        final int l_iteration;
        final int l_testcount;

        try
        (
            final InputStream l_stream = new FileInputStream( p_file )
        )
        {
            // convert source to stream
            final String l_source = IOUtils.toString( l_stream, Charset.defaultCharset() );

            // get test results from source
            final Matcher l_iterationmatch = ITERATION.matcher( l_source );
            l_iteration = l_iterationmatch.find()
                          ? Integer.parseInt( l_iterationmatch.group( 0 ).replace( ITERATIONNTAG, "" ).trim() )
                          : 1;

            final Matcher l_testcountmatcher = TESTCOUNT.matcher( l_source );
            l_testcount = l_testcountmatcher.find()
                          ? Integer.parseInt( l_testcountmatcher.group( 0 ).replace( TESTCOUNTTAG, "" ).trim() )
                          : 0;

            // generate agent
            l_agent = new CAgentGenerator(
                l_source,

                new CActionStaticGenerator(
                    Stream.concat(
                        Stream.of( new CTestPrint(), new CTestResult(), new CTestEqual(), new CTestToString(), new CTestIs() ),
                        CCommon.actionsFromPackage()
                    )
                ),

                new CLambdaStreamingStaticGenerator( CCommon.lambdastreamingFromPackage() ),

                ( p_agent, p_runningcontext ) -> Stream.of(
                    new CConstant<>( "MyConstInt", 123 ),
                    new CConstant<>( "MyConstString", "here is a test string" )
                )
            ).generatesingle();
        }
        catch ( final Exception l_exception )
        {
            l_exception.printStackTrace();
            Assert.fail( p_file );
            return;
        }

        IntStream.range( 0, l_iteration )
                 .forEach( i -> agentcycle( l_agent ) );

        Assert.assertEquals(
            MessageFormat.format( "{0} {1}", "number of tests", p_file ),
            l_testcount,
            m_count.get()
        );

    }

    // ---------------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * test action
     */
    private final class CTestResult extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 9032624165822970132L;
        /**
         * name
         */
        private final IPath m_name = CPath.of( "test/result" );

        @Nonnull
        @Override
        public IPath name()
        {
            return m_name;
        }

        @Nonnegative
        @Override
        public int minimalArgumentNumber()
        {
            return 1;
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                               @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
        {
            Assert.assertTrue(
                MessageFormat.format(
                    "{0}{1}{2}",
                    p_context.instance().literal(),
                    p_argument.size() > 1 ? ": " : "",
                    p_argument.size() > 1 ? p_argument.get( 1 ).raw() : ""
                ),
                p_argument.get( 0 ).<Boolean>raw()
            );
            m_count.incrementAndGet();
            return Stream.of();
        }
    }

    /**
     * print action for testing
     */
    private static final class CTestPrint extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 2845692684838132597L;
        /**
         * action name
         */
        private final IPath m_name = CPath.of( "test/print" );

        @Nonnull
        @Override
        public IPath name()
        {
            return m_name;
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context,
                                               @Nonnull final List<ITerm> p_argument, @Nonnull final List<ITerm> p_return )
        {
            if ( PRINTENABLE )
                System.out.println( p_argument.stream().map( Object::toString ).collect( Collectors.joining( "   " ) ) );

            return Stream.of();
        }
    }

    /**
     * test action for equality
     */
    private static final class CTestEqual extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = -2411595374105128516L;
        /**
         * action name
         */
        private final IPath m_name = CPath.of( "test/equal" );

        @Nonnull
        @Override
        public IPath name()
        {
            return m_name;
        }

        @Override
        public int minimalArgumentNumber()
        {
            return 2;
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context, @Nonnull final List<ITerm> p_argument,
                                               @Nonnull final List<ITerm> p_return
        )
        {
            if ( org.lightjason.agentspeak.language.CCommon.isssignableto( p_argument.get( 0 ), Collection.class ) )
                return this.pack(
                    p_return,
                    p_argument.stream()
                              .skip( 1 )
                              .map( i -> p_argument.get( 0 ).equals( i )
                                         || org.lightjason.agentspeak.language.CCommon.isssignableto( i, Collection.class )
                                            && equalcollection( p_argument.get( 0 ).<Collection<?>>raw().toArray(), i.raw() )
                              )
                );

            if ( org.lightjason.agentspeak.language.CCommon.isssignableto( p_argument.get( 0 ), Map.class ) )
                return this.pack(
                    p_return,
                    p_argument.stream()
                              .skip( 1 )
                              .map( i -> p_argument.get( 0 ).equals( i )
                                         || org.lightjason.agentspeak.language.CCommon.isssignableto( i, Map.class )
                                            && equalmap( p_argument.get( 0 ).raw(), i.raw() )
                              )
                );

            if ( org.lightjason.agentspeak.language.CCommon.isssignableto( p_argument.get( 0 ), Multimap.class ) )
                return this.pack(
                    p_return,
                    p_argument.stream()
                              .skip( 1 )
                              .map( i -> p_argument.get( 0 ).equals( i )
                                         || org.lightjason.agentspeak.language.CCommon.isssignableto( i, Multimap.class )
                                            && equalmultimap( p_argument.get( 0 ).raw(), i.raw() )
                              )
                );


            return this.pack(
                p_return,
                p_argument.stream()
                          .skip( 1 )
                          .map( i -> equalobject( p_argument.get( 0 ).<Object>raw(), i.<Object>raw() ) )
            );
        }

        /**
         * pack the result values into term
         *
         * @param p_return return item list
         * @param p_stream boolean input stream
         * @return boolean flag
         */
        private Stream<IFuzzyValue<?>> pack( @Nonnull final List<ITerm> p_return, @Nonnull final Stream<Boolean> p_stream )
        {
            p_stream.map( CRawTerm::of ).forEach( p_return::add );
            return Stream.of();
        }


        /**
         * compare any objects
         *
         * @param p_source source object
         * @param p_target object to compare
         * @return equality boolean flag
         */
        private static boolean equalobject( @Nonnull final Object p_source, @Nonnull final Object p_target )
        {
            return p_source.equals( p_target );
        }


        /**
         * compares collections
         *
         * @param p_source source array (converted collection to array)
         * @param p_target collection to compare
         * @return equality boolean flag
         */
        private static boolean equalcollection( @Nonnull final Object[] p_source, @Nonnull final Collection<?> p_target )
        {
            return Arrays.equals( p_source, p_target.toArray() );
        }


        /**
         * compare maps
         *
         * @param p_source source map
         * @param p_target map to compare
         * @return equality boolean flag
         */
        private static boolean equalmap( @Nonnull final Map<?, ?> p_source, @Nonnull final Map<?, ?> p_target )
        {
            return Arrays.equals( p_source.keySet().toArray(), p_target.keySet().toArray() )
                   && Arrays.equals( p_source.values().toArray(), p_target.values().toArray() );
        }


        /**
         * compare multimap
         *
         * @param p_source source multimap
         * @param p_target multimap to compare
         * @return equality boolean flag
         */
        private static boolean equalmultimap( @Nonnull final Multimap<?, ?> p_source, @Nonnull final Multimap<?, ?> p_target )
        {
            return Arrays.equals( p_source.asMap().keySet().toArray(), p_target.asMap().keySet().toArray() )
                   && Arrays.equals( p_source.values().toArray(), p_target.values().toArray() );
        }
    }

    /**
     * test to-string action
     */
    private static final class CTestToString extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 5939376997443562870L;
        /**
         * action name
         */
        private final IPath m_name = CPath.of( "test/tostring" );

        @Nonnull
        @Override
        public IPath name()
        {
            return m_name;
        }

        @Nonnull
        @Override
        public Stream<IFuzzyValue<?>> execute( final boolean p_parallel, @Nonnull final IContext p_context, @Nonnull final List<ITerm> p_argument,
                                               @Nonnull final List<ITerm> p_return )
        {
            p_argument.stream()
                      .map( ITerm::raw )
                      .map( i -> Objects.isNull( i ) ? "" : i.toString() )
                      .map( CRawTerm::of )
                      .forEach( p_return::add );

            return Stream.of();
        }
    }

    /**
     * test is action
     */
    private static final class CTestIs extends IBaseAction
    {
        /**
         * serial id
         */
        private static final long serialVersionUID = 1479225623609385322L;
        /**
         * action name
         */
        private final IPath m_name = CPath.of( "test/is" );

        @Nonnull
        @Override
        public IPath name()
        {
            return m_name;
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

}
