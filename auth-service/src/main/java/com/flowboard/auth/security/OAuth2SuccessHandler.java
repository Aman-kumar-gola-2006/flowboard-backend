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
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                         HttpServletResponse response, 
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        
        // If email is null (common in some GitHub profiles), try to get login
        if (email == null) {
            email = oAuth2User.getAttribute("login");
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(email);
        
        // Redirect to frontend with token
        response.sendRedirect("http://localhost:4200/oauth2/callback?token=" + token + "&name=" + name);
    }
}
