package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/check-active")
    public UserStatusResponse checkUserActive() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return new UserStatusResponse(false, false); // Not authenticated
        }

        String username = auth.getName();
        List<User> users = userRepository.findByUsername(username);
        
        if (users != null && !users.isEmpty()) {
            User user = users.get(0);
            return new UserStatusResponse(user.getIsActive(), true);
        } else {
            return new UserStatusResponse(false, false); // User not found
        }
    }

    public static class UserStatusResponse {
        private boolean active;
        private boolean authenticated;

        public UserStatusResponse(boolean active, boolean authenticated) {
            this.active = active;
            this.authenticated = authenticated;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public void setAuthenticated(boolean authenticated) {
            this.authenticated = authenticated;
        }
    }
}