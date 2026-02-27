package com.seismap.model.entity;

import com.seismap.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ApplicationSettings — embedded in Application entity.
 * Contains all default map parameters and layer server config.
 */
@Embeddable
public class ApplicationSettings {

    @Column(name = "settings_cache_expiration", nullable = false)
    private long settingsCacheExpiration;

    @Column(name = "layer_server_uri", nullable = false)
    private String layerServerUri;

    @Column(name = "google_maps_api_key", nullable = false)
    private String googleMapsApiKey;

    @Column(name = "event_map_zoom", nullable = false)
    private int eventMapZoom;

    @Column(name = "layer_name", nullable = false)
    private String layerName;

    @Column(name = "depth_layer_name", nullable = false)
    private String depthLayerName;

    @Column(name = "affected_distance_style_name", nullable = false)
    private String affectedDistanceStyleName;

    @Column(name = "default_map_name", nullable = false)
    private String defaultMapName;

    @Column(name = "default_map_description", nullable = false)
    private String defaultMapDescription;

    @Column(name = "default_map_center_longitude", nullable = false)
    private double defaultMapCenterLongitude;

    @Column(name = "default_map_center_latitude", nullable = false)
    private double defaultMapCenterLatitude;

    @Column(name = "default_map_zoom", nullable = false)
    private int defaultMapZoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_min_date_type", nullable = false)
    private DateLimitType defaultMapMinDateType;

    @Column(name = "default_map_min_date_relative_amount", nullable = false)
    private float defaultMapMinDateRelativeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_min_date_relative_units", nullable = false)
    private DateUnits defaultMapMinDateRelativeUnits;

    @Column(name = "default_map_min_date")
    private LocalDateTime defaultMapMinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_max_date_type", nullable = false)
    private DateLimitType defaultMapMaxDateType;

    @Column(name = "default_map_max_date_relative_amount", nullable = false)
    private float defaultMapMaxDateRelativeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_max_date_relative_units", nullable = false)
    private DateUnits defaultMapMaxDateRelativeUnits;

    @Column(name = "default_map_max_date")
    private LocalDateTime defaultMapMaxDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_min_depth_type", nullable = false)
    private DepthLimitType defaultMapMinDepthType;

    @Column(name = "default_map_min_depth")
    private Float defaultMapMinDepth;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_max_depth_type", nullable = false)
    private DepthLimitType defaultMapMaxDepthType;

    @Column(name = "default_map_max_depth")
    private Float defaultMapMaxDepth;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_magnitude_type", nullable = false)
    private ExtendedMagnitudeType defaultMapMagnitudeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_min_magnitude_type", nullable = false)
    private MagnitudeLimitType defaultMapMinMagnitudeType;

    @Column(name = "default_map_min_magnitude")
    private Float defaultMapMinMagnitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_max_magnitude_type", nullable = false)
    private MagnitudeLimitType defaultMapMaxMagnitudeType;

    @Column(name = "default_map_max_magnitude")
    private Float defaultMapMaxMagnitude;

    @Column(name = "default_map_list_unmeasured", nullable = false)
    private boolean defaultMapListUnmeasured;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_map_animation_type", nullable = false)
    private AnimationType defaultMapAnimationType;

    @Column(name = "default_map_animation_step_keep", nullable = false)
    private float defaultMapAnimationStepKeep;

    @Column(name = "default_map_animation_steps", nullable = false)
    private int defaultMapAnimationSteps;

    @Column(name = "default_map_animation_step_duration", nullable = false)
    private float defaultMapAnimationStepDuration;

    @Column(name = "default_map_reverse_animation", nullable = false)
    private boolean defaultMapReverseAnimation;

    @ManyToOne
    @JoinColumn(name = "default_map_style_id", nullable = false)
    private Style defaultMapStyle;

    @Column(name = "legends_directory", nullable = false)
    private String legendsDirectory;

    protected ApplicationSettings() {
    }

    // --- Getters & Setters ---
    public long getSettingsCacheExpiration() {
        return settingsCacheExpiration;
    }

    public void setSettingsCacheExpiration(long v) {
        this.settingsCacheExpiration = v;
    }

    public String getLayerServerUri() {
        return layerServerUri;
    }

    public void setLayerServerUri(String v) {
        this.layerServerUri = v;
    }

    public String getGoogleMapsApiKey() {
        return googleMapsApiKey;
    }

    public int getEventMapZoom() {
        return eventMapZoom;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getDepthLayerName() {
        return depthLayerName;
    }

    public String getAffectedDistanceStyleName() {
        return affectedDistanceStyleName;
    }

    public String getDefaultMapName() {
        return defaultMapName;
    }

    public String getDefaultMapDescription() {
        return defaultMapDescription;
    }

    public double getDefaultMapCenterLongitude() {
        return defaultMapCenterLongitude;
    }

    public double getDefaultMapCenterLatitude() {
        return defaultMapCenterLatitude;
    }

    public int getDefaultMapZoom() {
        return defaultMapZoom;
    }

    public Style getDefaultMapStyle() {
        return defaultMapStyle;
    }

    public String getLegendsDirectory() {
        return legendsDirectory;
    }
}
