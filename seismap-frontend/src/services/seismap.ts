import api from './api';
import type { SeismapMap } from '../types/map';
import type { EventSummary } from '../components/EventsWithinDialog';

export const mapService = {
    getDefault: (userId = 1) =>
        api.get<SeismapMap>(`/maps/default?userId=${userId}`).then(r => r.status === 204 ? null : r.data),

    getById: (id: number) =>
        api.get<SeismapMap>(`/maps/${id}`).then(r => r.data),

    listByUser: (userId = 1) =>
        api.get<SeismapMap[]>(`/maps?userId=${userId}`).then(r => r.data),

    create: (map: Partial<SeismapMap>) =>
        api.post<SeismapMap>('/maps', map).then(r => r.data),

    update: (id: number, map: Partial<SeismapMap>) =>
        api.put<SeismapMap>(`/maps/${id}`, map).then(r => r.data),

    rename: (id: number, name: string) =>
        api.patch<SeismapMap>(`/maps/${id}/name`, { name }).then(r => r.data),

    delete: (id: number) =>
        api.delete(`/maps/${id}`),
};

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface PolygonQuery {
    wkt: string;
    minDate?: string;
    maxDate?: string;
    minDepth?: number;
    maxDepth?: number;
    minMagnitude?: number;
    maxMagnitude?: number;
    page?: number;
    size?: number;
}

export const eventService = {
    getById: (id: number) =>
        api.get(`/events/${id}`).then(r => r.data),

    getDataBounds: () =>
        api.get('/events/data-bounds').then(r => r.data),

    getMagnitudeLimits: () =>
        api.get('/events/magnitude-limits').then(r => r.data),

    findWithin: (request: PolygonQuery): Promise<Page<EventSummary>> =>
        api.post<Page<EventSummary>>('/events/within', request).then(r => r.data),
};

export const styleService = {
    list: () => api.get('/styles').then(r => r.data),
};

export const categoryService = {
    list: () => api.get('/categories').then(r => r.data),
};

export const applicationService = {
    getSettings: () => api.get('/application/settings').then(r => r.data),
};

export const adminService = {
    listDataFiles: () => api.get('/admin/data-files').then(r => r.data),
    loadDataFile: (file: string) =>
        api.post('/admin/load-data-file', null, { params: { file } }).then(r => r.data),
};
