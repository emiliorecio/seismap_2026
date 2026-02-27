package com.seismap.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "agency", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    protected Agency() {
    }

    public Agency(String code) {
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
}
