package com.nuix.proserv.t3k;

public enum Endpoint {
    UPLOAD("upload"),
    POLL("poll/%s"),
    RESULT("result/%s"),
    SESSION("session/%s"),
    KEEP_ALIVE("keep-alive"),
    SEARCH("search/%s");

    private String endpointString;

    public String get(String... params) {
        if(null == params || 0 == params.length) return endpointString;
        else return String.format(endpointString, (Object[])params);
    }

    Endpoint(String endpoint) {
        this.endpointString = endpoint;
    }
}