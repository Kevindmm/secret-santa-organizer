package com.kdmm.secretsanta.model;

import jakarta.persistence.*;

@Entity
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String wishList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    private String assignedToEmail;  // Solo este campo
    private boolean emailSent = false;

    public Participant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWishList() { return wishList; }
    public void setWishList(String wishList) { this.wishList = wishList; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public String getAssignedToEmail() { return assignedToEmail; }
    public void setAssignedToEmail(String assignedToEmail) {
        this.assignedToEmail = assignedToEmail;
    }

    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }

    // helper
    public boolean isAssigned() {
        return assignedToEmail != null && !assignedToEmail.trim().isEmpty();
    }
}