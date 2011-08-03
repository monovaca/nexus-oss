package org.sonatype.nexus.test.booter;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusBooter
{
    protected static Logger log = LoggerFactory.getLogger( NexusBooter.class );

    // ==

    private static final String IT_REALM_ID = "it-realm";

    // ==

    private final File bundleBasedir;

    private final File sharedLibs;

    private final ClassWorld world;

    private final ClassRealm sharedClassloader;

    // ==

    private ClassRealm jetty7ClassLoader;

    private Object jetty7;

    private Method startJetty;

    private Method stopJetty;

    // private final Jetty7 jetty7;

    public NexusBooter( final File bundleBasedir, final int port )
        throws Exception
    {
        this.bundleBasedir = bundleBasedir;

        this.sharedLibs = new File( bundleBasedir.getParentFile(), "shared" );

        // modify the properties
        tamperJettyConfiguration( bundleBasedir, port );

        // shuffle bundle files
        tamperJarsForSharedClasspath( bundleBasedir );

        // set system property for bundleBasedir
        System.setProperty( "bundleBasedir", bundleBasedir.getAbsolutePath() );

        // guice finalizer
        System.setProperty( "guice.executor.class", "NONE" );

        // create ClassWorld
        world = new ClassWorld();

        // create shared loader
        sharedClassloader = buildSharedClassLoader();
    }

    protected Map<String, String> defaultContext( final File bundleBasedir )
    {
        Map<String, String> ctx = new HashMap<String, String>();
        ctx.put( "bundleBasedir", bundleBasedir.getAbsolutePath() );
        return ctx;
    }

    protected ClassRealm buildSharedClassLoader()
        throws Exception
    {
        // Circumvention of reloaded/recreated classloader. The Lucene NativeFSLockFactory is coded with expectation
        // that it might exist
        // only one class instance of it within one JVM. This is a limitation of JVM FileChannels + Lucene. Hence,
        // we "raise" the lucene classes level up.
        List<URL> urls = new ArrayList<URL>();

        final File[] jars = sharedLibs.listFiles();

        for ( File jar : jars )
        {
            urls.add( jar.toURI().toURL() );
        }

        ClassRealm realm = world.newRealm( "it-shared", null );

        for ( URL url : urls )
        {
            realm.addURL( url );
        }

        return realm;
    }

    protected ClassRealm buildNexusClassLoader( final File bundleBasedir, final String testId )
        throws Exception
    {
        List<URL> urls = new ArrayList<URL>();

        urls.add( new File( bundleBasedir, "runtime/apps/nexus/conf/" ).toURI().toURL() );

        final File libDir = new File( bundleBasedir, "runtime/apps/nexus/lib/" );

        final File[] jars = libDir.listFiles( new FileFilter()
        {
            @Override
            public boolean accept( File pathname )
            {
                return pathname.getName().endsWith( ".jar" );
            }
        } );

        for ( File jar : jars )
        {
            urls.add( jar.toURI().toURL() );
        }

        ClassRealm realm = world.newRealm( IT_REALM_ID + "-" + testId, sharedClassloader );

        for ( URL url : urls )
        {
            realm.addURL( url );
        }

        return realm;
    }

    protected void tamperJettyConfiguration( final File basedir, final int port )
        throws IOException
    {
        // ==
        // Set the port to the one expected by IT
        {
            final File jettyProperties = new File( basedir, "conf/jetty.properties" );

            if ( !jettyProperties.isFile() )
            {
                throw new FileNotFoundException( "Jetty properties not found at " + jettyProperties.getAbsolutePath() );
            }

            Properties p = new Properties();
            InputStream in = new FileInputStream( jettyProperties );
            p.load( in );
            IOUtil.close( in );

            p.setProperty( "application-port", String.valueOf( port ) );

            OutputStream out = new FileOutputStream( jettyProperties );
            p.store( out, "NexusStatusUtil" );
            IOUtil.close( out );
        }

        // ==
        // Disable the shutdown hook, since it disturbs the embedded work
        {
            final File jettyXml = new File( basedir, "conf/jetty.xml" );

            if ( !jettyXml.isFile() )
            {
                throw new FileNotFoundException( "Jetty properties not found at " + jettyXml.getAbsolutePath() );
            }

            String jettyXmlString = FileUtils.readFileToString( jettyXml, "UTF-8" );

            jettyXmlString =
                jettyXmlString.replace( "Set name=\"stopAtShutdown\">true", "Set name=\"stopAtShutdown\">false" );

            FileUtils.writeStringToFile( jettyXml, jettyXmlString, "UTF-8" );
        }
    }

    protected void tamperJarsForSharedClasspath( final File basedir )
        throws IOException
    {
        // Explanation, we filter for lucene-*.jar files that are bigger than one byte, in all directories below
        // bundleBasedir, since we
        // have to move them into /shared newly created folder to set up IT shared classpath.
        // But, we have to make it carefully, since we might be re-created during multi-forked ITs but the test-env
        // plugin unzips the nexus bundle only once
        // at the start of the build. So, we have to check and do it only once.
        tamperJarsForSharedClasspath( basedir, sharedLibs, "lucene-*.jar" );

        // LDAP does not unregister it? Like SISU container does not invoke Disposable.dispose() to make patch for
        // Provider unregistration happen? NO: the cause is if someone creates HTTPS connection while BC is registered,
        // JCEs SSLSocketFactory will get a grab on it. So, SISU is not faulty here, unregistration does happen, but
        // the URLConnection instance may still exists. So, we are lifting the provider into "shareds", and registering
        // it manually. LDAP's DefaultPlexusCipher obeys the registration rules, so will happily live with BC
        // registered.
        // WE ARE NOT REGISTERING IT ANYMORE, but is left here at "hand"
        // tamperJarsForSharedClasspath( basedir, sharedLibs, "bcprov-*.jar" );

        // logback
        // tamperJarsForSharedClasspath( basedir, sharedLibs, "logback-*.jar" );
    }

    protected void tamperJarsForSharedClasspath( final File basedir, final File sharedLibs, final String wildcard )
        throws IOException
    {
        @SuppressWarnings( "unchecked" )
        Collection<File> files =
            (Collection<File>) FileUtils.listFiles( basedir, new WildcardFileFilter( wildcard ), TrueFileFilter.TRUE );

        for ( File file : files )
        {
            // only if not in /shared folder and not zeroed
            if ( !file.getParentFile().equals( sharedLibs ) && file.length() > 0 )
            {
                // copy lucene jars to /shared
                FileUtils.copyFile( file, new File( sharedLibs, file.getName() ) );

                // replace lucene jars with dummies (to make nexus plugin manager happy) and prevent it's classes to be
                // loaded from non-shared classloader
                FileUtils.writeStringToFile( file, "" );
            }
        }
    }

    public void startNexus( final String testId )
        throws Exception
    {
        if ( startJetty != null )
        {
            // 2nd invocation? Stop first or puke?
            throw new IllegalStateException( "Nexus already started!" );
        }

        // create classloader
        jetty7ClassLoader = buildNexusClassLoader( bundleBasedir, testId );

        final ClassLoader original = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader( jetty7ClassLoader );

            final Class<?> jetty7Class = jetty7ClassLoader.loadClass( "org.sonatype.plexus.jetty.Jetty7" );

            jetty7 =
                jetty7Class.getConstructor( File.class, ClassLoader.class, Map[].class ).newInstance(
                    new File( bundleBasedir, "conf/jetty.xml" ), jetty7ClassLoader,
                    new Map[] { defaultContext( bundleBasedir ) } );

            startJetty = jetty7Class.getMethod( "startJetty" );
            stopJetty = jetty7Class.getMethod( "stopJetty" );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( original );
        }

        startJetty.invoke( jetty7 );
    }

    public void stopNexus()
        throws Exception
    {
        try
        {
            if ( stopJetty != null )
            {
                try
                {
                    stopJetty.invoke( jetty7 );
                }
                finally
                {
                    clean();
                }
            }
        }
        catch ( InvocationTargetException e )
        {
            if ( e.getCause() instanceof IllegalStateException )
            {
                // swallow it
            }
            else
            {
                throw (Exception) e.getCause();
            }
        }
    }

    protected void clean()
    {
        if ( jetty7ClassLoader != null )
        {
            try
            {
                world.disposeRealm( jetty7ClassLoader.getId() );
            }
            catch ( NoSuchRealmException e )
            {
                // huh?
            }
        }

        // drop references
        this.startJetty = null;
        this.stopJetty = null;
        this.jetty7 = null;
        this.jetty7ClassLoader = null;

        // force GC, may help
        System.gc();
    }
}
