package com.flowboard.label.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "card_labels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"card_id", "label_id"})
})
@Getter
@Setter
@ToString
public class CardLabel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(name = "label_id", nullable = false)
    private Long labelId;
}
