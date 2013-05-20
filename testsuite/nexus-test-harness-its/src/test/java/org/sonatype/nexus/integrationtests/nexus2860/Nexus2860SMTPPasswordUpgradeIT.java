/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.integrationtests.nexus2860;

import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;

public class Nexus2860SMTPPasswordUpgradeIT
    extends AbstractNexusIntegrationTest
{

    @Test
    public void upgradeSmtp()
        throws Exception
    {
        // we need this to have access to uncrypted password (see assertion below)
        String pw = getNexusConfigUtil().loadAndUpgradeNexusConfiguration().getSmtpConfiguration().getPassword();
        // ensuring it wasn't encrypted twice
        Assert.assertEquals( pw, "IT-password" );
    }
}