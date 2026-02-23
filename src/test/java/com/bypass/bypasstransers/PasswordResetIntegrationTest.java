package com.bypass.bypasstransers;

import com.bypass.bypasstransers.model.PasswordResetToken;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.PasswordResetTokenRepository;
import com.bypass.bypasstransers.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

@SpringBootTest
public class PasswordResetIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testForgotPasswordAndResetFlow() throws Exception {
        String username = "resetuser";
        String email = "resetuser@example.com";
        String rawPassword = "initialPass123";

        // create user
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(com.bypass.bypasstransers.enums.Role.STAFF);
        userRepository.save(u);

        // request forgot password
        mockMvc.perform(MockMvcRequestBuilders.post("/forgot-password")
                .param("emailOrUsername", email))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/forgot-password"));

        // token should be created
        List<PasswordResetToken> tokens = tokenRepository.findAll();
        Assertions.assertFalse(tokens.isEmpty(), "Password reset token should be created");
        PasswordResetToken token = tokens.get(0);
        Assertions.assertNotNull(token.getToken());

        // load reset form
        mockMvc.perform(MockMvcRequestBuilders.get("/reset").param("token", token.getToken()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Reset Password")));

        // submit new password
        String newPass = "newPassword123";
        mockMvc.perform(MockMvcRequestBuilders.post("/reset")
                .param("token", token.getToken())
                .param("password", newPass))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrlPattern("/login**"));

        // verify password updated
        User updated = userRepository.findByUsernameIgnoreCase(username).get(0);
        Assertions.assertTrue(passwordEncoder.matches(newPass, updated.getPassword()), "Password should be updated to the new value");
    }
}