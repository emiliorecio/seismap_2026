import React, { useEffect, useRef, useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Paper, Chip, Box, TablePagination, Tabs, Tab,
    CircularProgress
} from '@mui/material';
import type { Page } from '../services/seismap';
import PlaceIcon from '@mui/icons-material/Place';
import { toLonLat } from 'ol/proj';
import WKT from 'ol/format/WKT';
import Map from 'ol/Map';
import View from 'ol/View';
import ImageLayer from 'ol/layer/Image';
import ImageWMS from 'ol/source/ImageWMS';
import 'ol/ol.css';
import { useMapStore } from '../store/mapStore';
import { buildCqlFilter } from '../utils/cqlFilter';

const DEPTH_LAYER = 'seismap:eventandaveragemagnitudes_depthlocation';

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
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleString('es-AR', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit',
    });
}

const EventsWithinDialog: React.FC<Props> = ({ open, eventsPage, wkt, onClose, onPageChange, onPointClick }) => {
    const [tab, setTab] = useState(0);
    const [lonBounds, setLonBounds] = useState<[string, string]>(['', '']);
    const [crossSectionLoading, setCrossSectionLoading] = useState(true);

    const crossSectionMapDivRef = useRef<HTMLDivElement>(null);
    const crossSectionMapRef = useRef<Map | null>(null);

    const { currentMap } = useMapStore();

    const isPoints = currentMap?.style?.sld?.includes('points');
    const profileStyle = isPoints ? 'seismap_points_depth_profile' : 'seismap_circles_depth_profile';
    const legendUrl = `/api/maps/legend?name=${encodeURIComponent(profileStyle)}`;

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

        const map = new Map({
            target: crossSectionMapDivRef.current,
            layers: [new ImageLayer({ source: wmsSource })],
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
            const url = wmsSource.getFeatureInfoUrl(evt.coordinate, resolution, 'EPSG:3857', {
                INFO_FORMAT: 'application/json',
            });
            if (!url) return;
            fetch(url)
                .then((res) => res.json())
                .then((data) => {
                    const eventId = data?.features?.[0]?.properties?.id;
                    if (eventId && onPointClick) onPointClick(eventId);
                })
                .catch((err) => console.error('Failed to get cross-section feature info', err));
        });

        crossSectionMapRef.current = map;

        return () => {
            map.setTarget(undefined);
            crossSectionMapRef.current = null;
        };
    }, [tab, open, wkt, currentMap, profileStyle, onPointClick]);


    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth
            PaperProps={{ sx: { bgcolor: 'background.paper', height: '80vh', display: 'flex', flexDirection: 'column' } }}>
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1, pb: 1 }}>
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

            <DialogContent dividers sx={{ p: 0, overflow: 'hidden', flex: 1, display: 'flex', flexDirection: 'column' }}>
                {tab === 0 && (
                    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
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
                    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                        <Box sx={{ p: 1, bgcolor: '#f5f5f5', borderBottom: '1px solid #e0e0e0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <Typography variant="body2" color="text.secondary">Oeste {lonBounds[0]}°</Typography>
                            <Typography variant="body2" color="text.secondary">
                                <strong>Profundidad (0 a 750 km) v/s Longitud</strong> · clic en un punto para ver el detalle
                            </Typography>
                            <Typography variant="body2" color="text.secondary">Este {lonBounds[1]}°</Typography>
                        </Box>
                        <Box sx={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
                            <Box sx={{ flex: 1, position: 'relative', bgcolor: '#ffffff' }}>
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
                            <Box sx={{
                                width: 170, flexShrink: 0, borderLeft: '1px solid #e0e0e0',
                                bgcolor: '#ffffff', overflowY: 'auto', p: 1,
                            }}>
                                <Box component="img" src={legendUrl} alt="Leyenda" sx={{ width: '100%', display: 'block' }} />
                            </Box>
                        </Box>
                    </Box>
                )}
            </DialogContent>

            <DialogActions>
                <Button onClick={onClose} variant="contained" size="small">
                    Cerrar
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default EventsWithinDialog;
export type { EventSummary as EventSummaryType };
