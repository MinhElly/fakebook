package com.minh.fakebook.user.administration;

import java.util.*;
import java.util.function.Supplier;
/** Resolved afresh for each SQL execution, including count and ID loads. */
public class ExcludedSubjects implements Supplier<List<String>> {
    private final org.springframework.beans.factory.ObjectProvider<AccountDirectory> directory;
    public ExcludedSubjects(org.springframework.beans.factory.ObjectProvider<AccountDirectory> directory) { this.directory=directory; }
    @Override public List<String> get() {
        var ids=new ArrayList<>(directory.getObject().excluded());
        ids.add("00000000-0000-0000-0000-000000000000");
        return ids;
    }
}
