/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.bootstrap.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.bootstrap.CommandMonitorThread;

/**
 * Stops command monitor.
 *
 * @since 2.2
 */
public class StopMonitorCommand
    implements CommandMonitorThread.Command
{

    private static final Logger log = LoggerFactory.getLogger( StopMonitorCommand.class );

    public static final String STOP_MONITOR_COMMAND = "STOP_MONITOR";

    @Override
    public String getId()
    {
        return STOP_MONITOR_COMMAND;
    }

    @Override
    public boolean execute()
    {
        log.info( "Requesting monitor stop" );
        return true;
    }

}
