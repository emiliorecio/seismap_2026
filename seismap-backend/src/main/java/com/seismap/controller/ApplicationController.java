package com.seismap.controller;

import com.seismap.model.entity.Application;
import com.seismap.model.entity.ApplicationSettings;
import com.seismap.service.ApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public Application get() {
        return applicationService.getApplication();
    }

    @GetMapping("/settings")
    public ApplicationSettings getSettings() {
        return applicationService.getSettings();
    }
}
