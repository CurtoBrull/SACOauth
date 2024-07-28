package eu.jacurtobr.sacoauth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthorizationServerConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderShouldEncodePassword() {
        String rawPassword = "password";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void passwordEncoderShouldNotMatchDifferentPassword() {
        String rawPassword = "password";
        String differentPassword = "differentPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertFalse(passwordEncoder.matches(differentPassword, encodedPassword));
    }
}