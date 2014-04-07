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
/**
 * KeyNav controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.KeyNav', {
  extend: 'Ext.app.Controller',

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'form button[bindToEnter=true]': {
          afterrender: me.installEnterKey
        }
      }
    });
  },

  /**
   * @private
   * Install a key nav that will trigger click on any form buttons marked with "bindToEnter: true",
   * (usually submit button) on ENTER.
   */
  installEnterKey: function (button) {
    var form = button.up('form');

    button.keyNav = Ext.create('Ext.util.KeyNav', form.el, {
      enter: function () {
        if (!button.isDisabled()) {
          button.fireEvent('click', button);
        }
      }
    });
  }

});