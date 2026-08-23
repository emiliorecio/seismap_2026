package com.seismap.service;

import com.seismap.model.entity.Application;
import com.seismap.model.entity.ApplicationSettings;
import com.seismap.repository.ApplicationRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public Application getApplication() {
        Application application = applicationRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Application not configured"));
        // categories/styles (and each category's maps) are lazy and
        // open-in-view is disabled, so they must be initialized here, before
        // the session closes, or Jackson hits a LazyInitializationException
        // serializing the response.
        Hibernate.initialize(application.getCategories());
        application.getCategories().forEach(category -> Hibernate.initialize(category.getMaps()));
        Hibernate.initialize(application.getStyles());
        return application;
    }

    @Transactional(readOnly = true)
    public ApplicationSettings getSettings() {
        return getApplication().getApplicationSettings();
    }
}
