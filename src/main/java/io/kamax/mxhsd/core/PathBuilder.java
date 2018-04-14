/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxhsd.core;

import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

public class PathBuilder {

    private URIBuilder b;

    public PathBuilder() {
        b = new URIBuilder();
        b.setScheme("matrix");
        b.setHost("dummy");
    }

    public PathBuilder setPath(String path) {
        b.setPath(path);
        return this;
    }

    public PathBuilder addParameter(String key, String value) {
        b.addParameter(key, value);
        return this;
    }

    public String build() {
        try {
            return b.build().getPath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
