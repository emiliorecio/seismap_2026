package com.seismap.model.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "application")
public class Application {

    @Id
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "application_id", nullable = false)
    @OrderColumn(name = "in_application_index")
    private List<Category> categories = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "application_id", nullable = false)
    @OrderColumn(name = "in_application_index")
    private List<Style> styles = new ArrayList<>();

    @Embedded
    private ApplicationSettings applicationSettings;

    protected Application() {
    }

    public Long getId() {
        return id;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Style> getStyles() {
        return styles;
    }

    public ApplicationSettings getApplicationSettings() {
        return applicationSettings;
    }
}
