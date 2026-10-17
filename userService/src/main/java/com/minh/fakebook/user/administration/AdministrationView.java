package com.minh.fakebook.user.administration;

import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContextHolder;
public class AdministrationView implements Supplier<Boolean> {
    @Override public Boolean get() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        return auth!=null && auth.isAuthenticated() && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
