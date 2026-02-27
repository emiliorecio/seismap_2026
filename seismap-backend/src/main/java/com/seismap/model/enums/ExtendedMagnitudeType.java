package com.seismap.model.enums;

public enum ExtendedMagnitudeType {
    RANK(null),
    ML(MagnitudeType.ML),
    MB(MagnitudeType.MB),
    MS(MagnitudeType.MS),
    MW(MagnitudeType.MW),
    MBLG(MagnitudeType.MBLG),
    MC(MagnitudeType.MC);

    private final MagnitudeType magnitudeType;

    ExtendedMagnitudeType(MagnitudeType magnitudeType) {
        this.magnitudeType = magnitudeType;
    }

    public MagnitudeType getMagnitudeType() {
        return magnitudeType;
    }
}
