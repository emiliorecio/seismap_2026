package com.seismap.controller;

import com.seismap.model.entity.Style;
import com.seismap.service.StyleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/styles")
public class StyleController {

    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @GetMapping
    public List<Style> list() {
        return styleService.list();
    }

    @PostMapping
    public Style create(@RequestBody Style style) {
        return styleService.create(style);
    }
}
