/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd_test.api.federation;

import io.kamax.mxhsd.core.federation.FederationDomainResolver;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class FederationDomainResolverTest {

    private FederationDomainResolver getResolver() {
        FederationDomainResolver o = FederationDomainResolver.build().get();
        assertNotNull(o);
        return o;
    }

    @Test
    public void validIPv4Literal() {
        String address = "127.0.0.1";
        String expected = "127.0.0.1:8448";

        FederationDomainResolver o = getResolver();
        String resolved = o.resolve(address);

        assertTrue(resolved, StringUtils.equals(expected, resolved));
    }

    @Test
    public void validIPv6Literal() {
        String address = "::1";
        String expected = "[::1]:8448";

        FederationDomainResolver o = getResolver();
        String resolved = o.resolve(address);

        assertTrue(resolved, StringUtils.equals(expected, resolved));
    }

    @Test
    public void validHostnameWithValidPort() {
        String address = "localhost:1";
        String expected = "localhost:1";

        FederationDomainResolver o = getResolver();
        String resolved = o.resolve(address);

        assertTrue(resolved, StringUtils.equals(expected, resolved));
    }

    @Test
    public void validHostnameLiteral() {
        // FIXME do test
    }

}
