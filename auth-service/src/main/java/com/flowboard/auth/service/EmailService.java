package com.flowboard.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendResetPasswordEmail(String to, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("FlowBoard - Reset Your Password");
        message.setText(
            "Hello,\n\n" +
            "You requested to reset your password.\n\n" +
            "Click the link below to reset:\n" +
            "http://localhost:4200/reset-password?token=" + resetToken + "\n\n" +
            "This link will expire in 1 hour.\n\n" +
            "If you didn't request this, please ignore.\n\n" +
            "Thanks,\nFlowBoard Team"
        );
        mailSender.send(message);
        System.out.println("Reset email sent to: " + to);
    }
}
