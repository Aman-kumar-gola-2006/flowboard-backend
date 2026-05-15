package com.flowboard.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml);
            helper.setFrom(fromEmail);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendWelcomeEmail(String email, String name) {
        String content = "Welcome to FlowBoard! We're thrilled to have you join our community. FlowBoard is designed to help you organize your work and collaborate with your team faster than ever before.<br><br>Ready to dive in? Start by creating your first workspace and explore your new dashboard.";
        String html = getPremiumTemplate("Welcome to FlowBoard 🚀", name, content);
        sendEmail(email, "Welcome to FlowBoard 🚀 - Let's Start Building!", html, true);
    }

    private String getPremiumTemplate(String title, String name, String content) {
        return "<div style=\"font-family: 'Inter', Arial, sans-serif; background-color: #f8fafc; padding: 40px 20px; color: #1e293b;\">" +
                "  <div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 24px; overflow: hidden; border: 1px solid #e2e8f0;\">" +
                "    <div style=\"background-color: #4f46e5; padding: 40px; text-align: center;\">" +
                "      <h1 style=\"color: #ffffff; margin: 0; font-size: 32px; font-weight: 900; letter-spacing: -1px;\">FlowBoard<span style=\"opacity: 0.5;\">.</span></h1>" +
                "    </div>" +
                "    <div style=\"padding: 40px;\">" +
                "      <h2 style=\"font-size: 24px; font-weight: 800; color: #0f172a; margin-top: 0; margin-bottom: 20px;\">" + title + "</h2>" +
                "      <p style=\"font-size: 16px; line-height: 1.6; color: #475569; margin-bottom: 24px;\">Hi " + name + ",</p>" +
                "      <div style=\"font-size: 16px; line-height: 1.6; color: #475569; margin-bottom: 32px;\">" + content + "</div>" +
                "      <p style=\"font-size: 14px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 24px; margin-top: 32px;\">Best Regards,<br><strong style=\"color: #0f172a;\">The FlowBoard Team</strong></p>" +
                "    </div>" +
                "  </div>" +
                "  <div style=\"text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 2px;\">FlowBoard &copy; 2026</div>" +
                "</div>";
    }

    public void sendOtpEmail(String email, String otp) {
        String content = "Your One-Time Password (OTP) for security verification is below. This code is valid for <strong>10 minutes</strong>." +
                         "<div style=\"background-color: #f1f5f9; border-radius: 16px; padding: 24px; text-align: center; margin: 32px 0;\">" +
                         "  <span style=\"font-size: 36px; font-weight: 900; letter-spacing: 8px; color: #4f46e5;\">" + otp + "</span>" +
                         "</div>";
        String html = getPremiumTemplate("Security Verification", "User", content);
        sendEmail(email, "FlowBoard - Your Verification Code", html, true);
    }

    public void sendProUpgradeEmail(String email, String name) {
        String subject = "Congratulations! You're now a PRO Member";
        String body = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>" +
                      "<h1 style='color: #4f46e5;'>Congratulations, " + name + "!</h1>" +
                      "<p>Welcome to <strong>FlowBoard PRO</strong>!</p>" +
                      "<p>You now have access to all premium features, including:</p>" +
                      "<ul>" +
                      "<li>Unlimited Workspaces & Boards</li>" +
                      "<li>Advanced Analytics</li>" +
                      "<li>Priority Support</li>" +
                      "<li>Enhanced File Attachments</li>" +
                      "</ul>" +
                      "<p>Enjoy your upgraded experience!</p>" +
                      "<br><p>Best regards,<br>The FlowBoard Team</p>" +
                      "</div>";
        sendEmail(email, subject, body, true);
    }

    public void sendInvitationEmail(String email, String inviterName, String workspaceName, String inviteToken) {
        String inviteLink = "http://3.110.61.209:4200/invite?token=" + inviteToken;
        String subject = inviterName + " invited you to join " + workspaceName + " on FlowBoard";
        String body = "<h1>Workspace Invitation</h1>" +
                      "<p>Hi,</p>" +
                      "<p><strong>" + inviterName + "</strong> has invited you to collaborate on the workspace <strong>" + workspaceName + "</strong>.</p>" +
                      "<p><a href='" + inviteLink + "' style='background-color: #4f46e5; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Accept Invitation</a></p>" +
                      "<p>If the button doesn't work, copy and paste this link: " + inviteLink + "</p>" +
                      "<br><p>Best regards,<br>The FlowBoard Team</p>";
        sendEmail(email, subject, body, true);
    }

    public void sendTaskAssignmentEmail(String email, String name, String taskTitle, String boardName, String workspaceName) {
        String content = "A new task has been assigned to you:<br><br>" +
                         "<div style=\"background-color: #f1f5f9; border-radius: 16px; padding: 24px; margin: 32px 0;\">" +
                         "  <ul style=\"list-style: none; padding: 0; margin: 0;\">" +
                         "    <li style=\"margin-bottom: 12px;\"><strong>Task:</strong> " + taskTitle + "</li>" +
                         "    <li style=\"margin-bottom: 12px;\"><strong>Board:</strong> " + boardName + "</li>" +
                         "    <li><strong>Workspace:</strong> " + workspaceName + "</li>" +
                         "  </ul>" +
                         "</div>" +
                         "Log in to FlowBoard to view the details and start working.";
        String html = getPremiumTemplate("New Task Assigned: " + taskTitle, name, content);
        sendEmail(email, "New Task Assigned: " + taskTitle, html, true);
    }

    public void sendSuspensionEmail(String email, String name) {
        String content = "Your FlowBoard account has been <strong>blocked</strong> due to suspicious activity.<br><br>" +
                         "If you believe this is a mistake, please contact our support team at <a href='mailto:support@flowboard.com'>support@flowboard.com</a>.";
        String html = getPremiumTemplate("Account Suspended ⚠️", name, content);
        sendEmail(email, "Important: Your FlowBoard Account has been Suspended", html, true);
    }

    public void sendReactivationEmail(String email, String name) {
        String content = "Good news! Your FlowBoard account has been <strong>reactivated</strong>. You can now log in and resume your work where you left off.";
        String html = getPremiumTemplate("Account Reactivated ✨", name, content);
        sendEmail(email, "Your FlowBoard Account has been Reactivated", html, true);
    }

    public void sendSupportEmail(String name, String email, String subjectStr, String message) {
        String adminEmail = "amanagola9841@gmail.com";
        String subject = "FlowBoard Support: " + subjectStr;
        String body = "<h1>New Support Request</h1>" +
                      "<p><strong>From:</strong> " + name + " (" + email + ")</p>" +
                      "<p><strong>Message:</strong></p>" +
                      "<div style='background-color: #f8fafc; padding: 15px; border-left: 4px solid #4f46e5;'>" + message + "</div>" +
                      "<br><p>This is an automated notification.</p>";
        sendEmail(adminEmail, subject, body, true);
    }
}
