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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lightjason.agentspeak.action.IBaseAction;
import org.lightjason.agentspeak.agent.IAgent;
import org.lightjason.agentspeak.common.CCommon;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.common.IPath;
import org.lightjason.agentspeak.generator.CActionStaticGenerator;
import org.lightjason.agentspeak.generator.CLambdaStreamingStaticGenerator;
import org.lightjason.agentspeak.language.ITerm;
import org.lightjason.agentspeak.language.execution.IContext;
import org.lightjason.agentspeak.language.fuzzy.IFuzzyValue;
import org.lightjason.agentspeak.language.variable.CConstant;
import org.lightjason.agentspeak.testing.action.CTestAnd;
import org.lightjason.agentspeak.testing.action.CTestEqual;
import org.lightjason.agentspeak.testing.action.CTestIs;
import org.lightjason.agentspeak.testing.action.CTestListGet;
import org.lightjason.agentspeak.testing.action.CTestListRange;
import org.lightjason.agentspeak.testing.action.CTestOr;
import org.lightjason.agentspeak.testing.action.CTestPrint;
import org.lightjason.agentspeak.testing.action.CTestToString;

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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            return new Object[]{""};
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
        Assume.assumeFalse( "asl files does not exist", p_file.isEmpty() );

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
                        Stream.of(
                            new CTestPrint( PRINTENABLE ),
                            new CTestResult(),
                            new CTestEqual(),
                            new CTestToString(),
                            new CTestIs(),
                            new CTestListGet(),
                            new CTestListRange(),
                            new CTestAnd(),
                            new CTestOr()
                        ),
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
            return Stream.empty();
        }
    }

}
