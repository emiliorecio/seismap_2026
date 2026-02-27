package com.seismap.service;

import com.seismap.model.entity.Style;
import com.seismap.repository.StyleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StyleService {

    private final StyleRepository styleRepository;

    public StyleService(StyleRepository styleRepository) {
        this.styleRepository = styleRepository;
    }

    public List<Style> list() {
        return styleRepository.findAll();
    }

    public Style create(Style style) {
        return styleRepository.save(style);
    }
}
