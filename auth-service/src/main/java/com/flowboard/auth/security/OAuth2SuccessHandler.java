package com.flowboard.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private com.flowboard.auth.repository.UserRepository userRepository;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                         HttpServletResponse response, 
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String rawEmail = oAuth2User.getAttribute("email");
        if (rawEmail == null) {
            rawEmail = oAuth2User.getAttribute("login");
        }
        final String email = rawEmail;
        
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture"); // For Google
        
        // 1. Find or create user in database
        com.flowboard.auth.model.User user = userRepository.findByEmail(email).orElseGet(() -> {
            com.flowboard.auth.model.User newUser = new com.flowboard.auth.model.User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setUsername(email.split("@")[0]); // Default username
            newUser.setPassword(""); // No password for OAuth users
            newUser.setRole("MEMBER");
            newUser.setIsActive(true);
            newUser.setAvatarUrl(picture);
            newUser.setProvider("GOOGLE");
            return userRepository.save(newUser);
        });
        
        // 2. Generate full JWT token with ID
        String token = jwtUtil.generateToken(email, user.getId(), user.getRole(), user.getUsername());
        
        // 3. Redirect to frontend with token, name, and ID
        response.sendRedirect("http://localhost:4200/oauth2/callback?token=" + token + "&name=" + name + "&id=" + user.getId());
    }
}
