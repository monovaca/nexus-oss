/**
 * The OWASP CSRFGuard Project, BSD License
 * Eric Sheridan (eric@infraredsecurity.com), Copyright (c) 2011
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. Neither the name of OWASP nor the names of its contributors may be used
 *       to endorse or promote products derived from this software without specific
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sonatype.nexus.plugins.ui.internal.csrf;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.http.InterceptRedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @since 2.8.1
 */
@Named
@Singleton
public class CsrfGuardFilter
    implements Filter
{

  private static final Logger log = LoggerFactory.getLogger(CsrfGuard.class);

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
      throws IOException, ServletException
  {
    /** only work with HttpServletRequest objects **/
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpSession session = httpRequest.getSession(false);

      log.debug("Analyzing request {}", httpRequest.getRequestURI());

      if (session == null) {
        // If there is no session, no harm can be done
        filterChain.doFilter(httpRequest, response);
        return;
      }

      if (session.isNew()) {
        // if session is new put the token in response and skip verification as for sure it does not have a valid token
        CsrfGuard csrfGuard = CsrfGuard.getInstance();
        ((HttpServletResponse) response).setHeader(
            csrfGuard.getTokenName(),
            (String) session.getAttribute(csrfGuard.getSessionKey())
        );
        filterChain.doFilter(httpRequest, response);
        return;
      }

      CsrfGuard csrfGuard = CsrfGuard.getInstance();

      InterceptRedirectResponse httpResponse = new InterceptRedirectResponse(
          (HttpServletResponse) response, httpRequest, csrfGuard
      );

      if (csrfGuard.isValidRequest(httpRequest, httpResponse)) {
        filterChain.doFilter(httpRequest, httpResponse);
      }
      else {
        /** invalid request - nothing to do - actions already executed **/
      }
    }
    else {
      log.warn("CsrfGuard does not know how to work with requests of class {}", request.getClass().getName());
      filterChain.doFilter(request, response);
    }
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    // do nothing
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
