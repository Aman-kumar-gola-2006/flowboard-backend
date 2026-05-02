package com.flowboard.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;

    private String getHtmlTemplate(String title, String name, String content, String actionText, String actionUrl) {
        return "<div style=\"font-family: 'Inter', Arial, sans-serif; background-color: #f8fafc; padding: 40px 20px; color: #1e293b;\">" +
                "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 24px; overflow: hidden; shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0;\">" +
                "    <div style=\"background-color: #4f46e5; padding: 40px; text-align: center;\">" +
                "      <h1 style=\"color: #ffffff; margin: 0; font-size: 32px; font-weight: 900; letter-spacing: -1px;\">FlowBoard<span style=\"opacity: 0.5;\">.</span></h1>" +
                "    </div>" +
                "    <div style=\"padding: 40px;\">" +
                "      <h2 style=\"font-size: 24px; font-weight: 800; color: #0f172a; margin-top: 0; margin-bottom: 20px; tracking: -0.5px;\">" + title + "</h2>" +
                "      <p style=\"font-size: 16px; line-height: 1.6; color: #475569; margin-bottom: 24px;\">Hi " + name + ",</p>" +
                "      <div style=\"font-size: 16px; line-height: 1.6; color: #475569; margin-bottom: 32px;\">" + content + "</div>" +
                (actionUrl != null ? 
                "      <div style=\"text-align: center; margin-bottom: 32px;\">" +
                "        <a href=\"" + actionUrl + "\" style=\"background-color: #0f172a; color: #ffffff; padding: 16px 32px; border-radius: 16px; text-decoration: none; font-weight: 700; font-size: 14px; display: inline-block; text-transform: uppercase; letter-spacing: 1px;\">" + actionText + "</a>" +
                "      </div>" : "") +
                "      <p style=\"font-size: 14px; color: #94a3b8; border-top: 1px solid #e2e8f0; pt: 24px; margin-top: 32px;\">Best Regards,<br><strong style=\"color: #0f172a;\">The FlowBoard Team</strong></p>" +
                "    </div>" +
                "  </div>" +
                "  <div style=\"text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 2px;\">FlowBoard &copy; 2026 - Architected with Precision</div>" +
                "</div>";
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send HTML email: " + e.getMessage());
            throw new RuntimeException("Email delivery failed");
        }
    }
    
    public void sendOtpEmail(String to, String otp) {
        String content = "Your One-Time Password (OTP) for password reset is below. This code is valid for <strong>10 minutes</strong>. If you did not request this, please ignore this email safely." +
                         "<div style=\"background-color: #f1f5f9; border-radius: 16px; padding: 24px; text-align: center; margin: 32px 0;\">" +
                         "  <span style=\"font-size: 36px; font-weight: 900; letter-spacing: 8px; color: #4f46e5;\">" + otp + "</span>" +
                         "</div>";
        String html = getHtmlTemplate("Password Reset OTP", "User", content, null, null);
        sendHtmlEmail(to, "FlowBoard - Password Reset OTP", html);
    }

    public void sendWelcomeEmail(String to, String name) {
        String content = "Welcome to FlowBoard! We're thrilled to have you join our community. FlowBoard is designed to help you organize your work and collaborate with your team faster than ever before.<br><br>Ready to dive in? Start by creating your first workspace and explore your new dashboard.";
        String html = getHtmlTemplate("Welcome to FlowBoard 🚀", name, content, "Log In to Workspace", "http://localhost:4200/login");
        sendHtmlEmail(to, "Welcome to FlowBoard 🚀 - Let's Start Building!", html);
    }

    public void sendProUpgradeEmail(String to, String name) {
        String content = "Fantastic news! Your payment was successful, and your account has been upgraded to <strong>PRO status</strong>. You now have access to:<br><br>" +
                         "&bull; Unlimited Workspaces & Boards<br>" +
                         "&bull; Priority Support & Faster SLAs<br>" +
                         "&bull; Advanced Analytics & Custom Labels<br>" +
                         "&bull; Enhanced Storage & Privacy Settings";
        String html = getHtmlTemplate("You're a PRO Member! 💎", name, content, "Go to Dashboard", "http://localhost:4200/dashboard");
        sendHtmlEmail(to, "Congratulations! 💎 You're now a FlowBoard PRO Member", html);
    }

    public void sendSupportEmail(String fromName, String fromEmail, String subject, String problem) {
        // Internal support email can be simpler but still professional
        String content = "<strong>New Support Request</strong><br><br>" +
                         "<strong>User:</strong> " + fromName + " (" + fromEmail + ")<br>" +
                         "<strong>Subject:</strong> " + subject + "<br><br>" +
                         "<strong>Message:</strong><br>" + problem;
        String html = getHtmlTemplate("Support Request Received", "Admin", content, "Respond in Admin Panel", "http://localhost:4200/admin");
        sendHtmlEmail("amanagola9841@gmail.com", "FlowBoard Support: " + subject, html);
    }

    public void sendSuspensionEmail(String to, String name) {
        String content = "Important security update: Your account has been suspended due to suspicious activity detected on the platform. This action has been taken to protect your data and our community.<br><br>" +
                         "<strong>How to unblock your account:</strong><br>" +
                         "Please contact our support team immediately to verify your identity and request account reactivation. Our team is here to help you resolve this issue.";
        String html = getHtmlTemplate("Account Suspended ⚠️", name, content, "Contact Support", "mailto:amanagola9841@gmail.com");
        sendHtmlEmail(to, "Security Alert: Your FlowBoard account has been suspended", html);
    }

    public void sendReactivationEmail(String to, String name) {
        String content = "Great news! Your account has been reviewed and successfully reactivated. You can now log back in and continue organizing your projects with FlowBoard.<br><br>" +
                         "We appreciate your patience during the review process. Welcome back to the community!";
        String html = getHtmlTemplate("Account Reactivated 🎉", name, content, "Log In to FlowBoard", "http://localhost:4200/login");
        sendHtmlEmail(to, "Welcome Back! Your FlowBoard account is now active", html);
    }
}
