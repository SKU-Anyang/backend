package com.example.An_Yang.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 4000)
    private String answer;

    @Column(nullable = false)
    private LocalDateTime askedAt;

    public ChatHistory() { }

    public ChatHistory(String question, String answer, LocalDateTime askedAt) {
        this.question = question;
        this.answer = answer;
        this.askedAt = askedAt;
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public LocalDateTime getAskedAt() { return askedAt; }

    public void setId(Long id) { this.id = id; }
    public void setQuestion(String question) { this.question = question; }
    public void setAnswer(String answer) { this.answer = answer; }
    public void setAskedAt(LocalDateTime askedAt) { this.askedAt = askedAt; }
}
