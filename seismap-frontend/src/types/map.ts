export type DateLimitType = 'NONE' | 'RELATIVE' | 'ABSOLUTE';
export type DateUnits = 'MINUTE' | 'HOUR' | 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';
export type DepthLimitType = 'NONE' | 'ABSOLUTE';
export type MagnitudeLimitType = 'NONE' | 'ABSOLUTE';
export type AnimationType = 'NONE' | 'DATE' | 'DEPTH' | 'MAGNITUDE';
export type ExtendedMagnitudeType = 'RANK' | 'ML' | 'MB' | 'MS' | 'MW' | 'MBLG' | 'MC';

export interface Style {
    id: number;
    sld: string;
    name: string;
    variables: Record<string, string>;
}

export interface SeismapMap {
    id: number;
    name: string;
    description: string;
    zoom: number;
    center: { x: number; y: number };
    minDateType: DateLimitType;
    minDateRelativeAmount: number;
    minDateRelativeUnits: DateUnits;
    minDate: string;
    maxDateType: DateLimitType;
    maxDateRelativeAmount: number;
    maxDateRelativeUnits: DateUnits;
    maxDate: string;
    minDepthType: DepthLimitType;
    minDepth: number;
    maxDepthType: DepthLimitType;
    maxDepth: number;
    magnitudeType: number;
    minMagnitudeType: MagnitudeLimitType;
    minMagnitude: number;
    maxMagnitudeType: MagnitudeLimitType;
    maxMagnitude: number;
    listUnmeasured: boolean;
    animationType: AnimationType;
    animationStepKeep: number;
    animationSteps: number;
    animationStepDuration: number;
    reverseAnimation: boolean;
    style: Style;
}

export interface DataBounds {
    minDate: string | null;
    maxDate: string | null;
    minDepth: number | null;
    maxDepth: number | null;
}

export interface MagnitudeLimits {
    magnitudeType: ExtendedMagnitudeType;
    min: number;
    max: number;
}
