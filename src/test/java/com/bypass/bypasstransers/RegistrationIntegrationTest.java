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

import java.util.List;

@SpringBootTest
public class RegistrationIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void testRegisterCreatesUser() throws Exception {
        // Use unique username to avoid conflicts
        String username = "testuser_" + System.currentTimeMillis();
        String email = username + "@example.com";

        mockMvc.perform(MockMvcRequestBuilders.post("/register")
                .param("username", username)
                .param("email", email)
                .param("password", "password123"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/login"));

        List<User> users = userRepository.findByUsername(username);
        Assertions.assertFalse(users.isEmpty(), "User should be created");
        User u = users.get(0);
        Assertions.assertNotNull(u, "User should be created");
        Assertions.assertEquals(email, u.getEmail());
    }
}
