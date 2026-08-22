import { create } from 'zustand';
import type { SeismapMap, DataBounds } from '../types/map';

interface MapStore {
    currentMap: SeismapMap | null;
    savedMaps: SeismapMap[];
    dataBounds: DataBounds | null;
    selectedStyle: string;
    showLocalLayer: boolean;
    showUsgsLayer: boolean;
    setCurrentMap: (map: SeismapMap) => void;
    setSavedMaps: (maps: SeismapMap[]) => void;
    setDataBounds: (bounds: DataBounds) => void;
    setSelectedStyle: (style: string) => void;
    setShowLocalLayer: (show: boolean) => void;
    setShowUsgsLayer: (show: boolean) => void;
    updateCurrentMap: (patch: Partial<SeismapMap>) => void;
}

export const useMapStore = create<MapStore>((set) => ({
    currentMap: null,
    savedMaps: [],
    dataBounds: null,
    selectedStyle: 'seismap_circles_magnitude',
    showLocalLayer: true,
    showUsgsLayer: false,

    setCurrentMap: (map) => set({ currentMap: map }),
    setSavedMaps: (maps) => set({ savedMaps: maps }),
    setDataBounds: (bounds) => set({ dataBounds: bounds }),
    setSelectedStyle: (style) => set({ selectedStyle: style }),
    setShowLocalLayer: (show) => set({ showLocalLayer: show }),
    setShowUsgsLayer: (show) => set({ showUsgsLayer: show }),
    updateCurrentMap: (patch) =>
        set((state) =>
            state.currentMap ? { currentMap: { ...state.currentMap, ...patch } } : {}
        ),
}));
