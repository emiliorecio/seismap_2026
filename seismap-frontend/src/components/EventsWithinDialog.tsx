import React, { useEffect, useRef, useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Paper, Chip, Box, TablePagination, Tabs, Tab,
    CircularProgress
} from '@mui/material';
import type { Page } from '../services/seismap';
import PlaceIcon from '@mui/icons-material/Place';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import { toLonLat } from 'ol/proj';
import WKT from 'ol/format/WKT';
import Map from 'ol/Map';
import View from 'ol/View';
import ImageLayer from 'ol/layer/Image';
import ImageWMS from 'ol/source/ImageWMS';
import 'ol/ol.css';
import { useMapStore } from '../store/mapStore';
import { buildCqlFilter } from '../utils/cqlFilter';
import MapLegend from './MapLegend';
import type { UsgsEventProperties } from './UsgsEventDialog';

const DEPTH_LAYER = 'seismap:eventandaveragemagnitudes_depthlocation';
const USGS_DEPTH_LAYER = 'seismap:usgs_events_depthlocation';
const SIZE_STORAGE_KEY = 'seismap.eventsWithinDialog.size';
const POSITION_STORAGE_KEY = 'seismap.eventsWithinDialog.position';
const MIN_WIDTH = 520;
const MIN_HEIGHT = 400;

function loadStoredSize(): { width: number; height: number } {
    try {
        const raw = localStorage.getItem(SIZE_STORAGE_KEY);
        if (raw) {
            const parsed = JSON.parse(raw);
            if (typeof parsed.width === 'number' && typeof parsed.height === 'number') return parsed;
        }
    } catch {
        // ignore corrupt/inaccessible storage
    }
    return { width: 960, height: Math.round(window.innerHeight * 0.8) };
}

function loadStoredPosition(): { x: number; y: number } {
    try {
        const raw = localStorage.getItem(POSITION_STORAGE_KEY);
        if (raw) {
            const parsed = JSON.parse(raw);
            if (typeof parsed.x === 'number' && typeof parsed.y === 'number') return parsed;
        }
    } catch {
        // ignore corrupt/inaccessible storage
    }
    return { x: 0, y: 0 };
}

export interface EventSummary {
    id: number;
    date: string;
    depth: number;
    latitude: number;
    longitude: number;
    name?: string;
    reference?: string;
    rankMagnitude?: number;
}

interface Props {
    open: boolean;
    eventsPage: Page<EventSummary> | null;
    wkt: string | null;
    onClose: () => void;
    onPageChange: (newPage: number) => void;
    onPointClick?: (eventId: number) => void;
    onUsgsPointClick?: (properties: UsgsEventProperties) => void;
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleString('es-AR', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit',
    });
}

const EventsWithinDialog: React.FC<Props> = ({ open, eventsPage, wkt, onClose, onPageChange, onPointClick, onUsgsPointClick }) => {
    const [tab, setTab] = useState(0);
    const [lonBounds, setLonBounds] = useState<[string, string]>(['', '']);
    const [crossSectionLoading, setCrossSectionLoading] = useState(true);
    const [size, setSize] = useState(loadStoredSize);
    const [position, setPosition] = useState(loadStoredPosition);

    const crossSectionMapDivRef = useRef<HTMLDivElement>(null);
    const crossSectionMapRef = useRef<Map | null>(null);

    const handleResizeStart = (e: React.MouseEvent) => {
        e.preventDefault();
        const startX = e.clientX;
        const startY = e.clientY;
        const startW = size.width;
        const startH = size.height;

        const onMove = (ev: MouseEvent) => {
            const width = Math.min(Math.max(startW + (ev.clientX - startX), MIN_WIDTH), window.innerWidth - 32);
            const height = Math.min(Math.max(startH + (ev.clientY - startY), MIN_HEIGHT), window.innerHeight - 32);
            setSize({ width, height });
        };
        const onUp = () => {
            window.removeEventListener('mousemove', onMove);
            window.removeEventListener('mouseup', onUp);
            setSize((current) => {
                try {
                    localStorage.setItem(SIZE_STORAGE_KEY, JSON.stringify(current));
                } catch {
                    // ignore storage errors (private browsing, quota, etc.)
                }
                return current;
            });
        };
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onUp);
    };

    const handleDragStart = (e: React.MouseEvent) => {
        // Ignore drags starting on the close/action buttons in the title bar, if any are added later
        if ((e.target as HTMLElement).closest('button')) return;
        e.preventDefault();
        const startX = e.clientX;
        const startY = e.clientY;
        const startPosX = position.x;
        const startPosY = position.y;

        const onMove = (ev: MouseEvent) => {
            setPosition({ x: startPosX + (ev.clientX - startX), y: startPosY + (ev.clientY - startY) });
        };
        const onUp = () => {
            window.removeEventListener('mousemove', onMove);
            window.removeEventListener('mouseup', onUp);
            setPosition((current) => {
                try {
                    localStorage.setItem(POSITION_STORAGE_KEY, JSON.stringify(current));
                } catch {
                    // ignore storage errors (private browsing, quota, etc.)
                }
                return current;
            });
        };
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onUp);
    };

    const { currentMap, showUsgsLayer } = useMapStore();

    const isPoints = currentMap?.style?.sld?.includes('points');
    const profileStyle = isPoints ? 'seismap_points_depth_profile' : 'seismap_circles_depth_profile';

    // Reset tab on close
    useEffect(() => {
        if (!open) {
            setTab(0);
        }
    }, [open]);

    // Build the interactive cross-section map (ImageWMS on the depth-projected view)
    useEffect(() => {
        if (tab !== 1 || !open || !wkt || !crossSectionMapDivRef.current) return;

        setCrossSectionLoading(true);

        const format = new WKT();
        const feature = format.readFeature(wkt);

        let minX = 0, maxX = 0;
        const geom = feature.getGeometry();
        if (geom) {
            const extent = geom.getExtent();
            if (extent && extent.length === 4 && extent.every(isFinite)) {
                minX = extent[0];
                maxX = extent[2];
                if (minX >= maxX) {
                    minX -= 1000;
                    maxX += 1000;
                }
            }
        }

        const minLonLat = toLonLat([minX, 0], 'EPSG:3857');
        const maxLonLat = toLonLat([maxX, 0], 'EPSG:3857');
        setLonBounds([minLonLat[0].toFixed(2), maxLonLat[0].toFixed(2)]);

        const cqlParts: string[] = [`WITHIN(location, ${wkt})`];
        if (currentMap) {
            const mapCql = buildCqlFilter(currentMap);
            if (mapCql) cqlParts.push(`(${mapCql})`);
        }
        const cqlFilter = cqlParts.join(' AND ');

        const wmsSource = new ImageWMS({
            url: '/geoserver/seismap/wms',
            params: {
                LAYERS: DEPTH_LAYER,
                STYLES: profileStyle,
                CQL_FILTER: cqlFilter,
            },
            serverType: 'geoserver',
            ratio: 1,
        });
        wmsSource.on('imageloadend', () => setCrossSectionLoading(false));
        wmsSource.on('imageloaderror', () => setCrossSectionLoading(false));

        // USGS points share the same depth color scheme, filtered by the same
        // drawn polygon (but not the local catalog's magnitude/date filters,
        // whose fields don't exist on usgs_event).
        const usgsWmsSource = new ImageWMS({
            url: '/geoserver/seismap/wms',
            params: {
                LAYERS: USGS_DEPTH_LAYER,
                STYLES: 'usgs_depth_profile',
                CQL_FILTER: `WITHIN(location, ${wkt})`,
            },
            serverType: 'geoserver',
            ratio: 1,
        });

        const layers = [new ImageLayer({ source: wmsSource })];
        if (showUsgsLayer) {
            layers.push(new ImageLayer({ source: usgsWmsSource }));
        }

        const map = new Map({
            target: crossSectionMapDivRef.current,
            layers,
            view: new View({
                projection: 'EPSG:3857',
                center: [(minX + maxX) / 2, -375000],
                zoom: 2,
            }),
        });
        map.getView().fit([minX, -750000, maxX, 0], { size: map.getSize(), padding: [16, 16, 16, 16] });

        map.on('singleclick', (evt) => {
            const resolution = map.getView().getResolution();
            if (!resolution) return;

            const localUrl = wmsSource.getFeatureInfoUrl(evt.coordinate, resolution, 'EPSG:3857', {
                INFO_FORMAT: 'application/json',
            });
            if (!localUrl) return;

            fetch(localUrl)
                .then((res) => res.json())
                .then((data) => {
                    const eventId = data?.features?.[0]?.properties?.id;
                    if (eventId) {
                        onPointClick?.(eventId);
                        return;
                    }
                    if (!showUsgsLayer) return;
                    const usgsUrl = usgsWmsSource.getFeatureInfoUrl(evt.coordinate, resolution, 'EPSG:3857', {
                        INFO_FORMAT: 'application/json',
                    });
                    if (!usgsUrl) return;
                    fetch(usgsUrl)
                        .then((res) => res.json())
                        .then((data2) => {
                            const p = data2?.features?.[0]?.properties;
                            // This layer's default geometry is depthlocation (X=lon, Y=-depth),
                            // not the real position — that comes along as a secondary
                            // geometry-typed property instead.
                            const coords = p?.location?.coordinates;
                            if (p && coords) {
                                onUsgsPointClick?.({
                                    depth: p.depth, date: p.date, magnitude: p.magnitude,
                                    magnitude_type: p.magnitude_type, place: p.place, url: p.url,
                                    longitude: coords[0], latitude: coords[1],
                                });
                            }
                        })
                        .catch((err) => console.error('Failed to get USGS cross-section feature info', err));
                })
                .catch((err) => console.error('Failed to get cross-section feature info', err));
        });

        crossSectionMapRef.current = map;

        const resizeObserver = new ResizeObserver(() => map.updateSize());
        resizeObserver.observe(crossSectionMapDivRef.current);

        return () => {
            resizeObserver.disconnect();
            map.setTarget(undefined);
            crossSectionMapRef.current = null;
        };
    }, [tab, open, wkt, currentMap, profileStyle, showUsgsLayer, onPointClick, onUsgsPointClick]);


    return (
        <Dialog open={open} onClose={onClose} maxWidth={false}
            PaperProps={{
                sx: {
                    bgcolor: 'background.paper',
                    width: size.width,
                    height: size.height,
                    maxWidth: '95vw',
                    maxHeight: '95vh',
                    display: 'flex',
                    flexDirection: 'column',
                    position: 'relative',
                    overflow: 'hidden',
                    transform: `translate(${position.x}px, ${position.y}px)`,
                },
            }}>
            <DialogTitle
                onMouseDown={handleDragStart}
                sx={{ display: 'flex', alignItems: 'center', gap: 1, pb: 1, cursor: 'grab', userSelect: 'none', '&:active': { cursor: 'grabbing' } }}
            >
                <PlaceIcon color="primary" />
                Eventos en el área seleccionada
                <Chip label={eventsPage?.totalElements || 0} size="small" color="primary" sx={{ ml: 'auto' }} />
            </DialogTitle>

            <Tabs
                value={tab}
                onChange={(_, v) => setTab(v)}
                variant="fullWidth"
                textColor="primary"
                indicatorColor="primary"
                sx={{ borderBottom: 1, borderColor: 'divider' }}
            >
                <Tab label="Lista de Eventos" />
                <Tab label="Corte Transversal" />
            </Tabs>

            <DialogContent dividers sx={{ p: 0, overflow: 'hidden', flex: 1, minHeight: 0, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
                {tab === 0 && (
                    <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        {!eventsPage || eventsPage.content.length === 0 ? (
                            <Box sx={{ p: 4, textAlign: 'center' }}>
                                <Typography color="text.secondary">
                                    No se encontraron eventos en esta área.
                                </Typography>
                            </Box>
                        ) : (
                            <TableContainer component={Paper} elevation={0} sx={{ flex: 1, overflowY: 'auto' }}>
                                <Table size="small" stickyHeader>
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Fecha</TableCell>
                                            <TableCell align="right">Prof. (km)</TableCell>
                                            <TableCell align="right">Lat</TableCell>
                                            <TableCell align="right">Lon</TableCell>
                                            <TableCell>Nombre / Referencia</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {eventsPage.content.map(ev => {
                                            const [lonDeg, latDeg] = toLonLat([ev.longitude, ev.latitude], 'EPSG:3857');
                                            return (
                                                <TableRow key={ev.id} hover>
                                                    <TableCell sx={{ whiteSpace: 'nowrap' }}>
                                                        {formatDate(ev.date)}
                                                    </TableCell>
                                                    <TableCell align="right">{ev.depth.toFixed(1)}</TableCell>
                                                    <TableCell align="right">{latDeg.toFixed(4)}</TableCell>
                                                    <TableCell align="right">{lonDeg.toFixed(4)}</TableCell>
                                                    <TableCell>
                                                        <Typography variant="body2" noWrap>
                                                            {ev.name || ev.reference || '—'}
                                                        </Typography>
                                                    </TableCell>
                                                </TableRow>
                                            );
                                        })}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        )}
                        {eventsPage && eventsPage.totalElements > 0 && (
                            <TablePagination
                                component="div"
                                count={eventsPage.totalElements}
                                page={eventsPage.number}
                                onPageChange={(_, newPage) => onPageChange(newPage)}
                                rowsPerPage={eventsPage.size}
                                rowsPerPageOptions={[]} // keep fixed size to avoid complex logic
                            />
                        )}
                    </Box>
                )}

                {tab === 1 && (
                    <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                        <Box sx={{ p: 1, bgcolor: '#f5f5f5', borderBottom: '1px solid #e0e0e0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Typography variant="body2" color="text.secondary">Oeste {lonBounds[0]}°</Typography>
                            <Typography variant="body2" color="text.secondary">
                                <strong>Profundidad (0 a 750 km) v/s Longitud</strong> · clic en un punto para ver el detalle
                            </Typography>
                            <Typography variant="body2" color="text.secondary">Este {lonBounds[1]}°</Typography>
                        </Box>
                        <Box sx={{ flex: 1, minHeight: 0, position: 'relative', bgcolor: '#ffffff' }}>
                            <Box ref={crossSectionMapDivRef} sx={{ width: '100%', height: '100%', cursor: 'pointer' }} />
                            {crossSectionLoading && (
                                <Box sx={{
                                    position: 'absolute', inset: 0, display: 'flex',
                                    alignItems: 'center', justifyContent: 'center', bgcolor: 'rgba(255,255,255,0.6)',
                                }}>
                                    <CircularProgress />
                                </Box>
                            )}
                        </Box>
                        <MapLegend styleName={profileStyle} fixed />
                    </Box>
                )}
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} variant="contained" size="small">
                    Cerrar
                </Button>
            </DialogActions>

            <Box
                onMouseDown={handleResizeStart}
                title="Arrastrar para redimensionar"
                sx={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    width: 22,
                    height: 22,
                    cursor: 'nwse-resize',
                    display: 'flex',
                    alignItems: 'flex-end',
                    justifyContent: 'flex-end',
                    color: 'text.disabled',
                    zIndex: (t) => t.zIndex.modal + 1,
                }}
            >
                <DragIndicatorIcon sx={{ fontSize: 18, transform: 'rotate(45deg)', mb: '1px', mr: '1px' }} />
            </Box>
        </Dialog>
    );
};

export default EventsWithinDialog;
export type { EventSummary as EventSummaryType };
