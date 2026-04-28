package com.flowboard.board.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "card_labels")
@Getter
@Setter
public class CardLabel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "card_id", nullable = false)
    private Long cardId;
    
    @Column(name = "label_id", nullable = false)
    private Long labelId;
    
    @Column(name = "board_id", nullable = false)
    private Long boardId;
}
