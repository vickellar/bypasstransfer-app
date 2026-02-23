package com.bypass.bypasstransers;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
public class RegistrationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testRegisterCreatesUser() throws Exception {
        String username = "testuser1";
        String email = "testuser1@example.com";

        // Ensure user doesn't exist
        User existingByName = userRepository.findByUsernameIgnoreCase(username).stream().findFirst().orElse(null);
        if (existingByName != null) userRepository.delete(existingByName);
        User existingByEmail = userRepository.findByEmailIgnoreCase(email).stream().findFirst().orElse(null);
        if (existingByEmail != null) userRepository.delete(existingByEmail);

        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                .param("username", username)
                .param("email", email)
                .param("password", "password123"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));

        User u = userRepository.findByUsernameIgnoreCase(username).get(0);
        Assertions.assertNotNull(u, "User should be created");
        Assertions.assertEquals(email, u.getEmail());
    }
}