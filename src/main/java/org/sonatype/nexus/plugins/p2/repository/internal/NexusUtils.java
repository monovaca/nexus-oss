package org.sonatype.nexus.plugins.p2.repository.internal;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;

class NexusUtils
{

    private NexusUtils()
    {
    }

    static File retrieveFile( final Repository repository, final String path )
        throws LocalStorageException
    {
        final ResourceStoreRequest request = new ResourceStoreRequest( path );
        final File content =
            ( (DefaultFSLocalRepositoryStorage) repository.getLocalStorage() ).getFileFromBase( repository, request );
        return content;
    }

    static File safeRetrieveFile( final Repository repository, final String path )
    {
        try
        {
            return retrieveFile( repository, path );
        }
        catch ( final LocalStorageException e )
        {
            return null;
        }
    }

    static StorageItem retrieveItem( final Repository repository, final String path )
        throws Exception
    {
        final ResourceStoreRequest request = new ResourceStoreRequest( path );
        final StorageItem item = repository.retrieveItem( request );
        return item;
    }

    static StorageItem safeRetrieveItem( final Repository repository, final String path )
    {
        try
        {
            return retrieveItem( repository, path );
        }
        catch ( final Exception e )
        {
            return null;
        }
    }

    static void storeItem( final Repository repository, final ResourceStoreRequest request, final InputStream in,
                           final String mimeType, final Map<String, String> userAttributes )
        throws Exception
    {
        final DefaultStorageFileItem fItem =
            new DefaultStorageFileItem( repository, request, true, true, new PreparedContentLocator( in, mimeType ) );

        if ( userAttributes != null )
        {
            fItem.getAttributes().putAll( userAttributes );
        }

        repository.storeItem( false, fItem );
    }

    static void createLink( final Repository repository, final StorageItem item, final String path )
        throws Exception
    {
        final ResourceStoreRequest req = new ResourceStoreRequest( path );

        req.getRequestContext().putAll( item.getItemContext() );

        final DefaultStorageLinkItem link =
            new DefaultStorageLinkItem( repository, req, true, true, item.getRepositoryItemUid() );

        repository.storeItem( false, link );
    }

}