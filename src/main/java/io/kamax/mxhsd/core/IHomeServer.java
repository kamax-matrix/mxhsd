package io.kamax.mxhsd.core;

/**
 * Represent a Matrix Homeserver
 */
public interface IHomeServer {

    /**
     * Attempts to authenticate against this Homeserver.
     * <p>
     * <p>Raw credentials are given to the HS</p>
     *
     * @param rawCreds The raw credentials in a supported document format
     * @return The raw document output, which could contain an access token.
     */
    String login(String rawCreds);

    /**
     * Retrieve an authenticated user session with an access token.
     *
     * @param token Access token mapped to a user session
     * @return The user session
     */
    IUserSession getUserSession(String token);

}
