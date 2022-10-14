package com.nuix.proserv.t3k.ws.metadataprofile;

import lombok.*;

@ToString @EqualsAndHashCode @AllArgsConstructor
public class Metadata {

    @Getter
    private String type;

    @Getter
    private String name;

    @Getter
    private ScriptedExpression scriptedExpression;

    public Metadata() { this("SPECIAL", "Greet", null); }
}
