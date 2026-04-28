package com.flowboard.card.repository;

import com.flowboard.card.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByCardIdOrderByCreatedAtDesc(Long cardId);
    void deleteByCardId(Long cardId);
}
