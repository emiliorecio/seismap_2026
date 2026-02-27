import React, { useEffect, useRef, useState } from 'react';
import {
    Box,
    Drawer,
    AppBar,
    Toolbar,
    Typography,
    IconButton,
    Tabs,
    Tab,
    Tooltip,
    CircularProgress,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import MapIcon from '@mui/icons-material/Map';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PolylineIcon from '@mui/icons-material/Polyline';
import CancelIcon from '@mui/icons-material/Cancel';
import { Link } from 'react-router-dom';
import SeismapMapView from './SeismapMapView';
import MapControlsPanel from './MapControlsPanel';
import SavedMapsPanel from './SavedMapsPanel';
import MapLegend from './MapLegend';
import EventsWithinDialog from './EventsWithinDialog';
import EventDialog from './EventDialog';
import type { EventSummary } from './EventsWithinDialog';
import type { Page } from '../services/seismap';
import { useMapStore } from '../store/mapStore';
import { mapService, eventService } from '../services/seismap';
import { extractFilterBounds } from '../utils/cqlFilter';

const DRAWER_WIDTH = 320;

const MainLayout: React.FC = () => {
    const [open, setOpen] = useState(true);
    const [tab, setTab] = useState(0);
    const [drawingMode, setDrawingMode] = useState(false);
    const [loadingEvents, setLoadingEvents] = useState(false);
    const [eventsPage, setEventsPage] = useState<Page<EventSummary> | null>(null);
    const [currentWkt, setCurrentWkt] = useState<string | null>(null);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [eventDetailId, setEventDetailId] = useState<number | null>(null);
    const [eventDetailOpen, setEventDetailOpen] = useState(false);
    const clearPolygonRef = useRef<(() => void) | null>(null);

    const { currentMap, setCurrentMap, savedMaps, setSavedMaps, selectedStyle } = useMapStore();

    useEffect(() => {
        (async () => {
            try {
                const [defaultMap, maps] = await Promise.all([
                    mapService.getDefault(),
                    mapService.listByUser(),
                ]);
                if (defaultMap) {
                    setCurrentMap(defaultMap);
                } else {
                    setCurrentMap({
                        id: 0,
                        name: 'Mapa inicial',
                        description: '',
                        zoom: 5,
                        center: { x: -65, y: -32 },
                        minDateType: 'RELATIVE',
                        minDateRelativeAmount: 1,
                        minDateRelativeUnits: 'YEAR',
                        minDate: '',
                        maxDateType: 'NONE',
                        maxDateRelativeAmount: 0,
                        maxDateRelativeUnits: 'DAY',
                        maxDate: '',
                        minDepthType: 'NONE',
                        minDepth: 0,
                        maxDepthType: 'NONE',
                        maxDepth: 700,
                        magnitudeType: 1,
                        minMagnitudeType: 'NONE',
                        minMagnitude: 0,
                        maxMagnitudeType: 'NONE',
                        maxMagnitude: 10,
                        listUnmeasured: true,
                        animationType: 'NONE',
                        animationStepKeep: 1,
                        animationSteps: 10,
                        animationStepDuration: 1000,
                        reverseAnimation: false,
                        style: { id: 0, sld: 'seismap_default', name: 'seismap_default', variables: {} },
                    });
                }
                setSavedMaps(maps || []);
            } catch (err) {
                console.error('Failed to load initial map data', err);
            }
        })();
    }, []);

    const fetchPolygonEvents = async (wkt: string, page: number = 0) => {
        setLoadingEvents(true);
        try {
            const bounds = currentMap ? extractFilterBounds(currentMap) : {};
            const pageData = await eventService.findWithin({ wkt, page, size: 50, ...bounds });
            setEventsPage(pageData);
            setDialogOpen(true);
        } catch (err) {
            console.error('Failed to query events within polygon', err);
        } finally {
            setLoadingEvents(false);
        }
    };

    const handlePolygonComplete = (wkt: string) => {
        setDrawingMode(false);
        setCurrentWkt(wkt);
        fetchPolygonEvents(wkt, 0);
    };

    const handleClearPolygon = () => {
        clearPolygonRef.current?.();
        setEventsPage(null);
        setCurrentWkt(null);
    };

    const toggleDrawing = () => {
        if (drawingMode) {
            handleClearPolygon();
        }
        setDrawingMode(prev => !prev);
    };

    const handlePointClick = (eventId: number) => {
        setEventDetailId(eventId);
        setEventDetailOpen(true);
    };

    return (
        <Box sx={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
            <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
                <Toolbar variant="dense">
                    <IconButton color="inherit" onClick={() => setOpen(!open)} edge="start" sx={{ mr: 1 }}>
                        {open ? <ChevronLeftIcon /> : <MenuIcon />}
                    </IconButton>
                    <MapIcon sx={{ mr: 1 }} />
                    <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
                        {currentMap?.name ?? 'Seismap'}
                    </Typography>

                    {/* Polygon selection tool */}
                    <Tooltip title={drawingMode ? 'Cancelar selección' : 'Seleccionar área (polígono)'}>
                        <IconButton
                            color={drawingMode ? 'warning' : 'inherit'}
                            onClick={toggleDrawing}
                            size="small"
                            sx={{ mr: 0.5 }}
                        >
                            {loadingEvents
                                ? <CircularProgress size={20} color="inherit" />
                                : drawingMode
                                    ? <CancelIcon />
                                    : <PolylineIcon />}
                        </IconButton>
                    </Tooltip>

                    <Tooltip title="Administración">
                        <IconButton color="inherit" component={Link} to="/admin" size="small">
                            <AdminPanelSettingsIcon />
                        </IconButton>
                    </Tooltip>
                </Toolbar>
            </AppBar>

            <Drawer
                variant="persistent"
                open={open}
                sx={{
                    width: open ? DRAWER_WIDTH : 0,
                    flexShrink: 0,
                    transition: 'width 0.2s',
                    '& .MuiDrawer-paper': {
                        width: DRAWER_WIDTH,
                        boxSizing: 'border-box',
                        top: '40px',
                        height: 'calc(100% - 40px)',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                }}
            >
                <Tabs
                    value={tab}
                    onChange={(_, v) => setTab(v)}
                    variant="fullWidth"
                    textColor="inherit"
                    indicatorColor="primary"
                    sx={{ borderBottom: 1, borderColor: 'divider', minHeight: 36 }}
                >
                    <Tab label="Filtros" sx={{ minHeight: 36, py: 0 }} />
                    <Tab label={`Mapas (${savedMaps.length})`} sx={{ minHeight: 36, py: 0 }} />
                </Tabs>
                <Box sx={{ flex: 1, overflowY: 'auto' }}>
                    {tab === 0 && <MapControlsPanel />}
                    {tab === 1 && <SavedMapsPanel />}
                </Box>
            </Drawer>

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    height: '100vh',
                    pt: '40px',
                    position: 'relative',
                }}
            >
                <SeismapMapView
                    centerLon={currentMap?.center?.x ?? -65}
                    centerLat={currentMap?.center?.y ?? -32}
                    zoom={currentMap?.zoom ?? 5}
                    currentMap={currentMap}
                    styleName={selectedStyle}
                    drawingMode={drawingMode}
                    onPolygonComplete={handlePolygonComplete}
                    onClearPolygon={(fn) => { clearPolygonRef.current = fn; }}
                    onPointClick={handlePointClick}
                />
                <MapLegend styleName={selectedStyle} />

                {drawingMode && (
                    <Box sx={{
                        position: 'absolute', bottom: 24, left: '50%', transform: 'translateX(-50%)',
                        bgcolor: 'rgba(0,0,0,0.7)', color: '#fff', px: 2, py: 1,
                        borderRadius: 2, pointerEvents: 'none', fontSize: 13,
                    }}>
                        Hacé clic para dibujar el polígono · Doble clic para cerrar
                    </Box>
                )}
            </Box>

            <EventsWithinDialog
                open={dialogOpen}
                eventsPage={eventsPage}
                wkt={currentWkt}
                onClose={() => setDialogOpen(false)}
                onClearPolygon={handleClearPolygon}
                onPageChange={(newPage) => {
                    if (currentWkt) {
                        fetchPolygonEvents(currentWkt, newPage);
                    }
                }}
            />

            <EventDialog
                open={eventDetailOpen}
                eventId={eventDetailId}
                onClose={() => setEventDetailOpen(false)}
            />
        </Box>
    );
};

export default MainLayout;
