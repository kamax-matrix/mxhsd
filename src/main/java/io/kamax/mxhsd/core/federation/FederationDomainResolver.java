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

package io.kamax.mxhsd.core.federation;

import com.google.common.net.InetAddresses;
import io.kamax.mxhsd.api.federation.IFederationDomainResolver;
import io.kamax.mxhsd.api.federation.IRemoteAddress;
import org.xbill.DNS.*;

import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class FederationDomainResolver implements IFederationDomainResolver {

    public static class Builder {

        private FederationDomainResolver r = new FederationDomainResolver();

        public Builder withDefaultPort(int port) {
            r.port = port;
            return this;
        }

        public Builder withDnsPrefix(String prefix) {
            r.prefix = prefix;
            return this;
        }

        public FederationDomainResolver get() {
            try {
                return r;
            } finally {
                r = null;
            }
        }
    }

    public static Builder build() {
        return new Builder();
    }

    private int port = 8448;
    private String prefix = "_matrix._tcp.";

    private Optional<RemoteAddress> lookupSrv(String domain) {
        try {
            Record[] records = new Lookup(prefix + domain, Type.SRV).run();
            if (records == null) {
                return Optional.empty();
            }

            return Stream.of(records)
                    .filter(record -> record.getType() == Type.SRV && record instanceof SRVRecord)
                    .map(record -> (SRVRecord) record)
                    .min(Comparator.comparingInt(SRVRecord::getPriority))
                    .map(record -> new RemoteAddress(record.getTarget().toString(true), record.getPort()));
        } catch (TextParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public IRemoteAddress resolve(String domain) {
        // This is a literal IP address without any port
        // We add the default port and return the value
        if (InetAddresses.isInetAddress(domain)) {
            String host = InetAddresses.toUriString(InetAddresses.forString(domain));
            return new RemoteAddress(host, port);
        }

        // This is an IP address with a port in it
        // We return verbatim
        if (InetAddresses.isUriInetAddress(domain)) {
            URI v = URI.create(domain);
            return new RemoteAddress(v.getHost(), v.getPort());
        }

        if (domain.contains(":")) {
            URI v = URI.create("matrix://" + domain);
            return new RemoteAddress(v.getHost(), v.getPort());
        }

        return lookupSrv(domain).orElseGet(() -> new RemoteAddress(domain, port));
    }

}
