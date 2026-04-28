package com.flowboard.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("FlowBoard - Password Reset OTP");
        message.setText(
            "Hello,\n\n" +
            "Your One-Time Password (OTP) for password reset is:\n\n" +
            otp + "\n\n" +
            "This OTP will expire in 10 minutes.\n\n" +
            "If you didn't request this, please ignore.\n\n" +
            "Thanks,\nFlowBoard Team"
        );
        mailSender.send(message);
        System.out.println("OTP email sent to: " + to);
    }
}
