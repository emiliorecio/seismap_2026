package com.seismap.model.entity;

import jakarta.persistence.*;
import java.util.LinkedHashMap;

@Entity
@Table(name = "style")
public class Style {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sld;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "style_variable", joinColumns = @JoinColumn(name = "style_id"))
    @MapKeyColumn(name = "name")
    @Column(name = "value", nullable = false)
    private java.util.Map<String, String> variables = new LinkedHashMap<>();

    protected Style() {
    }

    public Style(String sld, String name, java.util.Map<String, String> variables) {
        this.sld = sld;
        this.name = name;
        this.variables = new LinkedHashMap<>(variables);
    }

    public Long getId() {
        return id;
    }

    public String getSld() {
        return sld;
    }

    public String getName() {
        return name;
    }

    public java.util.Map<String, String> getVariables() {
        return java.util.Collections.unmodifiableMap(variables);
    }
}
