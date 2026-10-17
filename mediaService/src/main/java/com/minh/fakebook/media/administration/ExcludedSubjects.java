package com.minh.fakebook.media.administration;

import java.util.*;
import java.util.function.Supplier;
/** Resolved afresh for each SQL execution, including count and ID loads. */
public class ExcludedSubjects implements Supplier<List<String>> {
    private final org.springframework.beans.factory.ObjectProvider<AccountDirectory> directory;
    public ExcludedSubjects(org.springframework.beans.factory.ObjectProvider<AccountDirectory> directory) { this.directory=directory; }
    @Override public List<String> get() {
        List<String> current;
        try {current=directory.getObject().excluded();}
        catch(Exception unavailable) {throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"ACCOUNT_STATE_UNAVAILABLE");}
        var ids=new ArrayList<>(current);
        ids.add("00000000-0000-0000-0000-000000000000");
        return ids;
    }
}
