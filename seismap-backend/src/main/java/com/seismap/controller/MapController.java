package com.seismap.controller;

import com.seismap.config.GeoServerProperties;
import com.seismap.model.entity.SeismapMap;
import com.seismap.service.MapService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maps")
public class MapController {

    private final MapService mapService;
    private final GeoServerProperties geoServerProps;
    private final RestClient geoServerClient;

    public MapController(MapService mapService, GeoServerProperties geoServerProps) {
        this.mapService = mapService;
        this.geoServerProps = geoServerProps;
        this.geoServerClient = RestClient.builder()
                .baseUrl(geoServerProps.getUrl())
                .build();
    }

    @GetMapping("/default")
    public ResponseEntity<SeismapMap> getDefault(@RequestParam(defaultValue = "1") Long userId) {
        return mapService.getDefault(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public SeismapMap get(@PathVariable Long id) {
        return mapService.getById(id);
    }

    @GetMapping
    public List<SeismapMap> listByUser(@RequestParam(defaultValue = "1") Long userId) {
        return mapService.listByUser(userId);
    }

    @PostMapping
    public SeismapMap create(@RequestBody SeismapMap map) {
        return mapService.create(map);
    }

    @PatchMapping("/{id}/name")
    public SeismapMap rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return mapService.rename(id, body.get("name"));
    }

    @PutMapping("/{id}")
    public SeismapMap update(@PathVariable Long id, @RequestBody SeismapMap map) {
        return mapService.update(id, map);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        mapService.delete(id);
    }

    /**
     * Proxy GetLegendGraphic from GeoServer or serve static legend images.
     * Usage: GET /api/maps/legend?name=seismap_circles_magnitude
     */
    @GetMapping("/legend")
    public ResponseEntity<byte[]> getLegend(@RequestParam String name) {
        String staticFilename = mapToStaticLegendFilename(name);
        if (staticFilename != null) {
            try {
                org.springframework.core.io.ClassPathResource imgFile = new org.springframework.core.io.ClassPathResource(
                        "legends/" + staticFilename);
                if (imgFile.exists()) {
                    byte[] bytes = org.springframework.util.StreamUtils.copyToByteArray(imgFile.getInputStream());
                    MediaType mediaType = staticFilename.endsWith(".svg") ? MediaType.valueOf("image/svg+xml")
                            : MediaType.IMAGE_PNG;
                    return ResponseEntity.ok()
                            .contentType(mediaType)
                            .body(bytes);
                }
            } catch (java.io.IOException e) {
                // Fallback to GeoServer on error
            }
        }

        String layer = geoServerProps.getWorkspace() + ":eventandaveragemagnitudes";
        byte[] image = geoServerClient.get()
                .uri("/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetLegendGraphic"
                        + "&LAYER=" + layer
                        + "&STYLE=" + name
                        + "&FORMAT=image/png"
                        + "&WIDTH=20&HEIGHT=20")
                .retrieve()
                .body(byte[].class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().headers(headers).body(image);
    }

    private String mapToStaticLegendFilename(String styleName) {
        return switch (styleName) {
            case "seismap_circles_magnitude", "seismap_points_magnitude" -> "seismap_leyenda_magnitud.svg";
            case "seismap_circles_depth", "seismap_points_depth" -> "seismap_leyenda_profundidad.svg";
            case "seismap_circles_age", "seismap_points_age" -> "seismap_leyenda_antiguedad.svg";
            default -> null;
        };
    }
}
