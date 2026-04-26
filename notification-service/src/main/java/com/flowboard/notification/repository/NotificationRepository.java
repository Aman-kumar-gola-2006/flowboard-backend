package com.flowboard.notification.repository;

import com.flowboard.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);
    
    Long countByRecipientIdAndIsReadFalse(Long recipientId);
    
    void deleteByRecipientIdAndIsReadTrue(Long recipientId);
}
