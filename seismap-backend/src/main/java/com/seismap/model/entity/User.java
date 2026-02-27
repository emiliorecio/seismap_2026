package com.seismap.model.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seismap_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean administrator;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    @OrderColumn(name = "in_user_index")
    private List<SeismapMap> maps = new ArrayList<>();

    protected User() {
    }

    public User(String name, String email, String passwordHash, boolean administrator) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.administrator = administrator;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public List<SeismapMap> getMaps() {
        return maps;
    }
}
