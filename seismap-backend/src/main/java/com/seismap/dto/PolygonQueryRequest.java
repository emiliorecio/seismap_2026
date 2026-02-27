package com.seismap.dto;

import java.time.LocalDateTime;

public class PolygonQueryRequest {
    /** WKT polygon string in EPSG:900913, e.g. POLYGON((x1 y1, x2 y2, ...)) */
    private String wkt;

    // Optional absolute filters calculated by the frontend
    private LocalDateTime minDate;
    private LocalDateTime maxDate;
    private Float minDepth;
    private Float maxDepth;
    private Float minMagnitude;
    private Float maxMagnitude;

    // Pagination
    private int page = 0;
    private int size = 50;

    public PolygonQueryRequest() {
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    public LocalDateTime getMinDate() {
        return minDate;
    }

    public void setMinDate(LocalDateTime minDate) {
        this.minDate = minDate;
    }

    public LocalDateTime getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(LocalDateTime maxDate) {
        this.maxDate = maxDate;
    }

    public Float getMinDepth() {
        return minDepth;
    }

    public void setMinDepth(Float minDepth) {
        this.minDepth = minDepth;
    }

    public Float getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Float maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Float getMinMagnitude() {
        return minMagnitude;
    }

    public void setMinMagnitude(Float minMagnitude) {
        this.minMagnitude = minMagnitude;
    }

    public Float getMaxMagnitude() {
        return maxMagnitude;
    }

    public void setMaxMagnitude(Float maxMagnitude) {
        this.maxMagnitude = maxMagnitude;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
