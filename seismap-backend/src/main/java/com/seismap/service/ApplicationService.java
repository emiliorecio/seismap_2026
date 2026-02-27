package com.seismap.service;

import com.seismap.model.entity.Application;
import com.seismap.model.entity.ApplicationSettings;
import com.seismap.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public Application getApplication() {
        return applicationRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Application not configured"));
    }

    public ApplicationSettings getSettings() {
        return getApplication().getApplicationSettings();
    }
}
