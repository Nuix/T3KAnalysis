package com.nuix.proserv.t3k;

/**
 * Holds constants for the endpoints to the T3K Server.
 * <p>
 *     Some of the endpoints take a parameter, in which case they are represented as a Format String.  You can retrieve
 *     values of the endpoints as such:
 * </p>
 * <pre>
 *     String uploadEndpoint = Endpoint.UPLOAD.get(); // no parameters for the endpoint
 *     String pollEndpoint = Endpoint.POLL.get(1); // An endpoint with a parameter.
 * </pre>
 */
public enum Endpoint {
    //
    UPLOAD("upload"),
    POLL("poll/%s"),
    RESULT("result/%s"),
    SESSION("session/%s"),
    KEEP_ALIVE("keep-alive"),
    SEARCH("search/%s");

    private String endpointString;

    /**
     * Get the formatted endpoint.
     * @param params The, possibly null / empty list of parameters to insert in the endpoint.  The parameters must be in
     *               the order required by the endpoint.
     * @return The formatted endpoint, or the unformatted endpoint string if no parameters are provided.
     */
    public String get(String... params) {
        if(null == params || 0 == params.length) return endpointString;
        else return String.format(endpointString, (Object[])params);
    }

    Endpoint(String endpoint) {
        this.endpointString = endpoint;
    }
}