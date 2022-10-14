package com.nuix.proserv.t3k.ws.metadataprofile;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;

@EqualsAndHashCode
public class MetadataProfile {

    @Getter
    private Set<Metadata> columns = new LinkedHashSet<>();

}
