import { create } from 'zustand';
import type { SeismapMap, DataBounds } from '../types/map';

interface MapStore {
    currentMap: SeismapMap | null;
    savedMaps: SeismapMap[];
    dataBounds: DataBounds | null;
    selectedStyle: string;
    setCurrentMap: (map: SeismapMap) => void;
    setSavedMaps: (maps: SeismapMap[]) => void;
    setDataBounds: (bounds: DataBounds) => void;
    setSelectedStyle: (style: string) => void;
    updateCurrentMap: (patch: Partial<SeismapMap>) => void;
}

export const useMapStore = create<MapStore>((set) => ({
    currentMap: null,
    savedMaps: [],
    dataBounds: null,
    selectedStyle: 'seismap_circles_magnitude',

    setCurrentMap: (map) => set({ currentMap: map }),
    setSavedMaps: (maps) => set({ savedMaps: maps }),
    setDataBounds: (bounds) => set({ dataBounds: bounds }),
    setSelectedStyle: (style) => set({ selectedStyle: style }),
    updateCurrentMap: (patch) =>
        set((state) =>
            state.currentMap ? { currentMap: { ...state.currentMap, ...patch } } : {}
        ),
}));
