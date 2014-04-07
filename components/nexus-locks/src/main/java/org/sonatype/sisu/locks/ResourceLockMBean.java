/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.sisu.locks;

/**
 * JMX API for managing and monitoring resource locks.
 */
public interface ResourceLockMBean
{
  /**
   * @return Names of currently allocated resource locks
   */
  String[] listResourceNames();

  /**
   * @return Identities of threads that own the named resource lock
   */
  String[] findOwningThreads(String name);

  /**
   * @return Identities of threads waiting for the named resource lock
   */
  String[] findWaitingThreads(String name);

  /**
   * @return Names of resource locks owned by the given thread
   */
  String[] findOwnedResources(String tid);

  /**
   * @return Names of resource locks wanted by the given thread
   */
  String[] findWaitedResources(String tid);

  /**
   * Forcibly releases the named resource lock; use with caution
   */
  void releaseResource(String name);
}
