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

package io.kamax.mxhsd.api;

import io.kamax.mxhsd.api.session.IUserSession;

import java.util.Optional;

/**
 * Represent a Matrix Homeserver
 */
public interface IHomeServer {

    String getDomain();

    /**
     * Attempts to authenticate against this Homeserver.
     *
     * @param username The username of the user
     * @param password The password of the user
     * @return The newly created session
     */
    IUserSession login(String username, char[] password);

    /**
     * Retrieve an authenticated user session with an access token.
     *
     * @param token Access token mapped to a user session
     * @return The user session
     */
    IUserSession getUserSession(String token);

    /**
     * Find a possible authenticated user session with an access token.
     *
     * @param token Access token mapped to a user session
     * @return The user session, if one exists
     */
    Optional<IUserSession> findUserSession(String token);

}
