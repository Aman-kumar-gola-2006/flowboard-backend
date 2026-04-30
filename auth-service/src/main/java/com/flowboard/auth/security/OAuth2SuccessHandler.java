package com.flowboard.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.flowboard.auth.service.EmailService;
import com.flowboard.auth.service.MessageProducer;
import com.flowboard.auth.dto.NotificationMessage;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private com.flowboard.auth.repository.UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MessageProducer messageProducer;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                         HttpServletResponse response, 
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        
        String rawEmail = oAuth2User.getAttribute("email");
        if (rawEmail == null) {
            rawEmail = oAuth2User.getAttribute("login");
        }
        
        if (rawEmail == null) {
            // If still null, we can't proceed. Redirect with error.
            response.sendRedirect("http://localhost:4200/login?error=oauth2");
            return;
        }
        
        final String email = rawEmail;
        
        String name = oAuth2User.getAttribute("name");
        if (name == null) {
            name = oAuth2User.getAttribute("given_name");
            if (name == null) name = email.split("@")[0];
        }
        
        String picture = oAuth2User.getAttribute("picture"); // For Google
        if (picture == null) {
            picture = oAuth2User.getAttribute("avatar_url"); // For GitHub
        }
        
        final String finalName = name;
        final String finalPicture = picture;
        
        final boolean[] isNewUser = {false};
        // 1. Find or create user in database
        com.flowboard.auth.model.User user = userRepository.findByEmail(email).orElseGet(() -> {
            isNewUser[0] = true;
            com.flowboard.auth.model.User newUser = new com.flowboard.auth.model.User();
            newUser.setEmail(email);
            newUser.setFullName(finalName);
            
            // Unique Username Generation
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
            if (baseUsername.length() < 3) baseUsername += "user";
            
            String targetUsername = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(targetUsername)) {
                targetUsername = baseUsername + (System.currentTimeMillis() % 1000) + counter++;
            }
            
            newUser.setUsername(targetUsername);
            // Satisfaction of @NotBlank with a secure random string (user won't use it for login)
            newUser.setPassword(java.util.UUID.randomUUID().toString()); 
            newUser.setRole("MEMBER");
            newUser.setIsActive(true);
            newUser.setEmailVerified(true); // Trusted provider
            newUser.setAvatarUrl(finalPicture);
            newUser.setProvider(registrationId.toUpperCase());
            
            // Capture Provider ID (sub for Google, id for others)
            Object sub = oAuth2User.getAttribute("sub");
            if (sub == null) sub = oAuth2User.getAttribute("id");
            if (sub != null) newUser.setProviderId(sub.toString());
            
            return userRepository.save(newUser);
        });

        // 1.2 Send Welcome Email via RabbitMQ if new user
        if (isNewUser[0]) {
            try {
                messageProducer.sendNotification(new NotificationMessage(user.getEmail(), user.getFullName(), "WELCOME", null));
            } catch (Exception e) {
                System.err.println("Failed to queue welcome email for social user: " + e.getMessage());
            }
        }
        
        // 1.5 Check if user is suspended
        if (!user.getIsActive()) {
            response.sendRedirect("http://localhost:4200/login?error=suspended");
            return;
        }
        
        // 2. Generate full JWT token with ID
        String token = jwtUtil.generateToken(email, user.getId(), user.getRole(), user.getUsername());
        
        // 3. Redirect to frontend with token, name, and ID
        response.sendRedirect("http://localhost:4200/oauth2/callback?token=" + token + "&name=" + finalName + "&id=" + user.getId());
    }
}
