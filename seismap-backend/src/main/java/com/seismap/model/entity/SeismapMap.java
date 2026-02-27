package com.seismap.model.entity;

import com.seismap.model.enums.*;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

/**
 * SeismapMap entity — named to avoid conflict with java.util.Map.
 * Maps to the "map" table.
 */
@Entity
@Table(name = "map")
public class SeismapMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "in_category_index", insertable = false, updatable = false)
    private Integer inCategoryIndex;

    @Column(name = "in_user_index", insertable = false, updatable = false)
    private Integer inUserIndex;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, columnDefinition = "geometry(Point, 900913)")
    private Point center;

    @Column(nullable = false)
    private int zoom;

    // --- Date filters ---
    @Enumerated(EnumType.STRING)
    @Column(name = "min_date_type", nullable = false)
    private DateLimitType minDateType;

    @Column(name = "min_date_relative_amount", nullable = false)
    private float minDateRelativeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_date_relative_units", nullable = false)
    private DateUnits minDateRelativeUnits;

    @Column(name = "min_date", nullable = false)
    private LocalDateTime minDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_date_type", nullable = false)
    private DateLimitType maxDateType;

    @Column(name = "max_date_relative_amount", nullable = false)
    private float maxDateRelativeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_date_relative_units", nullable = false)
    private DateUnits maxDateRelativeUnits;

    @Column(name = "max_date", nullable = false)
    private LocalDateTime maxDate;

    // --- Depth filters ---
    @Enumerated(EnumType.STRING)
    @Column(name = "min_depth_type", nullable = false)
    private DepthLimitType minDepthType;

    @Column(name = "min_depth")
    private float minDepth;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_depth_type", nullable = false)
    private DepthLimitType maxDepthType;

    @Column(name = "max_depth")
    private float maxDepth;

    // --- Magnitude filters ---
    @Column(name = "magnitude_type", nullable = false)
    private int magnitudeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_magnitude_type", nullable = false)
    private MagnitudeLimitType minMagnitudeType;

    @Column(name = "min_magnitude")
    private float minMagnitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_magnitude_type", nullable = false)
    private MagnitudeLimitType maxMagnitudeType;

    @Column(name = "max_magnitude")
    private float maxMagnitude;

    @Column(name = "list_unmeasured", nullable = false)
    private boolean listUnmeasured;

    // --- Animation ---
    @Enumerated(EnumType.STRING)
    @Column(name = "animation_type", nullable = false)
    private AnimationType animationType;

    @Column(name = "animation_step_keep", nullable = false)
    private Float animationStepKeep;

    @Column(name = "animation_steps", nullable = false)
    private int animationSteps;

    @Column(name = "animation_step_duration", nullable = false)
    private float animationStepDuration;

    @Column(name = "reverse_animation", nullable = false)
    private boolean reverseAnimation;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    protected SeismapMap() {
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    public DateLimitType getMinDateType() {
        return minDateType;
    }

    public void setMinDateType(DateLimitType minDateType) {
        this.minDateType = minDateType;
    }

    public float getMinDateRelativeAmount() {
        return minDateRelativeAmount;
    }

    public void setMinDateRelativeAmount(float v) {
        this.minDateRelativeAmount = v;
    }

    public DateUnits getMinDateRelativeUnits() {
        return minDateRelativeUnits;
    }

    public void setMinDateRelativeUnits(DateUnits v) {
        this.minDateRelativeUnits = v;
    }

    public LocalDateTime getMinDate() {
        return minDate;
    }

    public void setMinDate(LocalDateTime v) {
        this.minDate = v;
    }

    public DateLimitType getMaxDateType() {
        return maxDateType;
    }

    public void setMaxDateType(DateLimitType v) {
        this.maxDateType = v;
    }

    public float getMaxDateRelativeAmount() {
        return maxDateRelativeAmount;
    }

    public void setMaxDateRelativeAmount(float v) {
        this.maxDateRelativeAmount = v;
    }

    public DateUnits getMaxDateRelativeUnits() {
        return maxDateRelativeUnits;
    }

    public void setMaxDateRelativeUnits(DateUnits v) {
        this.maxDateRelativeUnits = v;
    }

    public LocalDateTime getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(LocalDateTime v) {
        this.maxDate = v;
    }

    public DepthLimitType getMinDepthType() {
        return minDepthType;
    }

    public void setMinDepthType(DepthLimitType v) {
        this.minDepthType = v;
    }

    public float getMinDepth() {
        return minDepth;
    }

    public void setMinDepth(float v) {
        this.minDepth = v;
    }

    public DepthLimitType getMaxDepthType() {
        return maxDepthType;
    }

    public void setMaxDepthType(DepthLimitType v) {
        this.maxDepthType = v;
    }

    public float getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(float v) {
        this.maxDepth = v;
    }

    public int getMagnitudeType() {
        return magnitudeType;
    }

    public void setMagnitudeType(int v) {
        this.magnitudeType = v;
    }

    public MagnitudeLimitType getMinMagnitudeType() {
        return minMagnitudeType;
    }

    public void setMinMagnitudeType(MagnitudeLimitType v) {
        this.minMagnitudeType = v;
    }

    public float getMinMagnitude() {
        return minMagnitude;
    }

    public void setMinMagnitude(float v) {
        this.minMagnitude = v;
    }

    public MagnitudeLimitType getMaxMagnitudeType() {
        return maxMagnitudeType;
    }

    public void setMaxMagnitudeType(MagnitudeLimitType v) {
        this.maxMagnitudeType = v;
    }

    public float getMaxMagnitude() {
        return maxMagnitude;
    }

    public void setMaxMagnitude(float v) {
        this.maxMagnitude = v;
    }

    public boolean isListUnmeasured() {
        return listUnmeasured;
    }

    public void setListUnmeasured(boolean v) {
        this.listUnmeasured = v;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }

    public void setAnimationType(AnimationType v) {
        this.animationType = v;
    }

    public Float getAnimationStepKeep() {
        return animationStepKeep;
    }

    public void setAnimationStepKeep(Float v) {
        this.animationStepKeep = v;
    }

    public int getAnimationSteps() {
        return animationSteps;
    }

    public void setAnimationSteps(int v) {
        this.animationSteps = v;
    }

    public float getAnimationStepDuration() {
        return animationStepDuration;
    }

    public void setAnimationStepDuration(float v) {
        this.animationStepDuration = v;
    }

    public boolean isReverseAnimation() {
        return reverseAnimation;
    }

    public void setReverseAnimation(boolean v) {
        this.reverseAnimation = v;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public boolean isPublic() {
        return inCategoryIndex != null;
    }
}
