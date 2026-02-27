package com.seismap.controller;

import com.seismap.service.DataLoadService;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DataLoadService dataLoadService;

    public AdminController(DataLoadService dataLoadService) {
        this.dataLoadService = dataLoadService;
    }

    @GetMapping("/data-files")
    public List<Map<String, String>> listDataFiles() {
        return dataLoadService.listDataFiles().stream()
                .map(f -> Map.of(
                        "name", f.getName(),
                        "size", String.valueOf(f.length())))
                .collect(Collectors.toList());
    }

    @PostMapping("/load-data-file")
    public Map<String, Object> loadDataFile(@RequestParam String file) throws IOException {
        return dataLoadService.loadDataFile(file);
    }
}
