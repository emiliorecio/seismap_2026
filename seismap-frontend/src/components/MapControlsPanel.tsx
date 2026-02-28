import React from 'react';
import {
    Box,
    Typography,
    Select,
    MenuItem,
    FormControl,
    InputLabel,
    Slider,
    Stack,
    Switch,
    FormControlLabel,
    Divider,
    Accordion,
    AccordionSummary,
    AccordionDetails,
    Button,
    CircularProgress,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import PolylineIcon from '@mui/icons-material/Polyline';
import CancelIcon from '@mui/icons-material/Cancel';
import { useMapStore } from '../store/mapStore';
import type { DateLimitType, DepthLimitType, MagnitudeLimitType } from '../types/map';

interface MapControlsPanelProps {
    drawingMode: boolean;
    loadingEvents: boolean;
    onToggleDrawing: () => void;
}

const MapControlsPanel: React.FC<MapControlsPanelProps> = ({ drawingMode, loadingEvents, onToggleDrawing }) => {
    const { currentMap, updateCurrentMap, selectedStyle, setSelectedStyle } = useMapStore();

    if (!currentMap) {
        return (
            <Box sx={{ p: 2 }}>
                <Typography variant="body2" color="text.secondary">
                    Cargando mapa...
                </Typography>
            </Box>
        );
    }

    return (
        <Box sx={{ px: 1, py: 0.5 }}>

            {/* Date Filter */}
            <Accordion defaultExpanded disableGutters elevation={0}
                sx={{ background: 'transparent' }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="subtitle2">Fecha</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{ pt: 0 }}>
                    <Stack spacing={3}>
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo mín.</InputLabel>
                            <Select<DateLimitType>
                                value={currentMap.minDateType}
                                label="Tipo mín."
                                onChange={(e) => updateCurrentMap({ minDateType: e.target.value as DateLimitType })}
                            >
                                <MenuItem value="NONE">Sin límite</MenuItem>
                                <MenuItem value="RELATIVE">Relativo</MenuItem>
                                <MenuItem value="ABSOLUTE">Absoluto</MenuItem>
                            </Select>
                        </FormControl>
                        {currentMap.minDateType === 'ABSOLUTE' && (
                            <input
                                type="datetime-local"
                                value={currentMap.minDate ? currentMap.minDate.substring(0, 16) : ''}
                                onChange={(e) => updateCurrentMap({ minDate: e.target.value + ':00' })}
                                style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                            />
                        )}
                        {currentMap.minDateType === 'RELATIVE' && (
                            <Stack direction="row" spacing={1}>
                                <input
                                    type="number"
                                    min="0"
                                    value={currentMap.minDateRelativeAmount || 0}
                                    onChange={(e) => updateCurrentMap({ minDateRelativeAmount: Number(e.target.value) })}
                                    style={{ width: '50%', padding: '8px', boxSizing: 'border-box' }}
                                />
                                <Select
                                    size="small"
                                    value={currentMap.minDateRelativeUnits || 'DAY'}
                                    onChange={(e) => updateCurrentMap({ minDateRelativeUnits: e.target.value as any })}
                                    sx={{ width: '50%' }}
                                >
                                    <MenuItem value="MINUTE">Minutos</MenuItem>
                                    <MenuItem value="HOUR">Horas</MenuItem>
                                    <MenuItem value="DAY">Días</MenuItem>
                                    <MenuItem value="WEEK">Semanas</MenuItem>
                                    <MenuItem value="MONTH">Meses</MenuItem>
                                    <MenuItem value="YEAR">Años</MenuItem>
                                </Select>
                            </Stack>
                        )}
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo máx.</InputLabel>
                            <Select<DateLimitType>
                                value={currentMap.maxDateType}
                                label="Tipo máx."
                                onChange={(e) => updateCurrentMap({ maxDateType: e.target.value as DateLimitType })}
                            >
                                <MenuItem value="NONE">Sin límite</MenuItem>
                                <MenuItem value="RELATIVE">Relativo</MenuItem>
                                <MenuItem value="ABSOLUTE">Absoluto</MenuItem>
                            </Select>
                        </FormControl>
                        {currentMap.maxDateType === 'ABSOLUTE' && (
                            <input
                                type="datetime-local"
                                value={currentMap.maxDate ? currentMap.maxDate.substring(0, 16) : ''}
                                onChange={(e) => updateCurrentMap({ maxDate: e.target.value + ':00' })}
                                style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                            />
                        )}
                        {currentMap.maxDateType === 'RELATIVE' && (
                            <Stack direction="row" spacing={1}>
                                <input
                                    type="number"
                                    min="0"
                                    value={currentMap.maxDateRelativeAmount || 0}
                                    onChange={(e) => updateCurrentMap({ maxDateRelativeAmount: Number(e.target.value) })}
                                    style={{ width: '50%', padding: '8px', boxSizing: 'border-box' }}
                                />
                                <Select
                                    size="small"
                                    value={currentMap.maxDateRelativeUnits || 'DAY'}
                                    onChange={(e) => updateCurrentMap({ maxDateRelativeUnits: e.target.value as any })}
                                    sx={{ width: '50%' }}
                                >
                                    <MenuItem value="MINUTE">Minutos</MenuItem>
                                    <MenuItem value="HOUR">Horas</MenuItem>
                                    <MenuItem value="DAY">Días</MenuItem>
                                    <MenuItem value="WEEK">Semanas</MenuItem>
                                    <MenuItem value="MONTH">Meses</MenuItem>
                                    <MenuItem value="YEAR">Años</MenuItem>
                                </Select>
                            </Stack>
                        )}
                    </Stack>
                </AccordionDetails>
            </Accordion>

            <Divider />

            {/* Depth Filter */}
            <Accordion disableGutters elevation={0} sx={{ background: 'transparent' }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="subtitle2">Profundidad (km)</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{ pt: 0 }}>
                    <Stack spacing={3}>
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo mín.</InputLabel>
                            <Select<DepthLimitType>
                                value={currentMap.minDepthType}
                                label="Tipo mín."
                                onChange={(e) => updateCurrentMap({ minDepthType: e.target.value as DepthLimitType })}
                            >
                                <MenuItem value="NONE">Sin límite</MenuItem>
                                <MenuItem value="ABSOLUTE">Absoluto</MenuItem>
                            </Select>
                        </FormControl>
                        {currentMap.minDepthType === 'ABSOLUTE' && (
                            <Box>
                                <Typography variant="caption">Min: {currentMap.minDepth} km</Typography>
                                <Slider
                                    size="small"
                                    min={0} max={700} step={5}
                                    value={currentMap.minDepth}
                                    onChange={(_, v) => updateCurrentMap({ minDepth: v as number })}
                                />
                            </Box>
                        )}
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo máx.</InputLabel>
                            <Select<DepthLimitType>
                                value={currentMap.maxDepthType}
                                label="Tipo máx."
                                onChange={(e) => updateCurrentMap({ maxDepthType: e.target.value as DepthLimitType })}
                            >
                                <MenuItem value="NONE">Sin límite</MenuItem>
                                <MenuItem value="ABSOLUTE">Absoluto</MenuItem>
                            </Select>
                        </FormControl>
                        {currentMap.maxDepthType === 'ABSOLUTE' && (
                            <Box>
                                <Typography variant="caption">Max: {currentMap.maxDepth} km</Typography>
                                <Slider
                                    size="small"
                                    min={0} max={700} step={5}
                                    value={currentMap.maxDepth}
                                    onChange={(_, v) => updateCurrentMap({ maxDepth: v as number })}
                                />
                            </Box>
                        )}
                    </Stack>
                </AccordionDetails>
            </Accordion>

            <Divider />

            {/* Magnitude Filter */}
            <Accordion disableGutters elevation={0} sx={{ background: 'transparent' }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="subtitle2">Magnitud</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{ pt: 0 }}>
                    <Stack spacing={3}>
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo mín.</InputLabel>
                            <Select<MagnitudeLimitType>
                                value={currentMap.minMagnitudeType}
                                label="Tipo mín."
                                onChange={(e) => updateCurrentMap({ minMagnitudeType: e.target.value as MagnitudeLimitType })}
                            >
                                <MenuItem value="NONE">Sin límite</MenuItem>
                                <MenuItem value="ABSOLUTE">Absoluto</MenuItem>
                            </Select>
                        </FormControl>
                        {currentMap.minMagnitudeType === 'ABSOLUTE' && (
                            <Box>
                                <Typography variant="caption">Min: {currentMap.minMagnitude}</Typography>
                                <Slider size="small" min={0} max={10} step={0.1} value={currentMap.minMagnitude}
                                    onChange={(_, v) => updateCurrentMap({ minMagnitude: v as number })} />
                            </Box>
                        )}
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo máx.</InputLabel>
                            <Select<MagnitudeLimitType>
                                value={currentMap.maxMagnitudeType}
                                label="Tipo máx."
                                onChange={(e) => updateCurrentMap({ maxMagnitudeType: e.target.value as MagnitudeLimitType })}
                            >
                                <MenuItem value="NONE">Sin límite</MenuItem>
                                <MenuItem value="ABSOLUTE">Absoluto</MenuItem>
                            </Select>
                        </FormControl>
                        {currentMap.maxMagnitudeType === 'ABSOLUTE' && (
                            <Box>
                                <Typography variant="caption">Max: {currentMap.maxMagnitude}</Typography>
                                <Slider size="small" min={0} max={10} step={0.1} value={currentMap.maxMagnitude}
                                    onChange={(_, v) => updateCurrentMap({ maxMagnitude: v as number })} />
                            </Box>
                        )}
                        <FormControlLabel
                            control={
                                <Switch
                                    size="small"
                                    checked={currentMap.listUnmeasured}
                                    onChange={(e) => updateCurrentMap({ listUnmeasured: e.target.checked })}
                                />
                            }
                            label={<Typography variant="caption">Incluir sin magnitud</Typography>}
                        />
                    </Stack>
                </AccordionDetails>
            </Accordion>

            <Divider />

            {/* Animation (Hidden for now)
            <Accordion disableGutters elevation={0} sx={{ background: 'transparent' }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="subtitle2">Animación</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{ pt: 0 }}>
                    <Stack spacing={1}>
                        <FormControl size="small" fullWidth>
                            <InputLabel>Tipo</InputLabel>
                            <Select<AnimationType>
                                value={currentMap.animationType}
                                label="Tipo"
                                onChange={(e) => updateCurrentMap({ animationType: e.target.value as AnimationType })}
                            >
                                <MenuItem value="NONE">Sin animación</MenuItem>
                                <MenuItem value="DATE">Por fecha</MenuItem>
                                <MenuItem value="DEPTH">Por profundidad</MenuItem>
                                <MenuItem value="MAGNITUDE">Por magnitud</MenuItem>
                            </Select>
                        </FormControl>
                        <FormControlLabel
                            control={
                                <Switch size="small" checked={currentMap.reverseAnimation}
                                    onChange={(e) => updateCurrentMap({ reverseAnimation: e.target.checked })} />
                            }
                            label={<Typography variant="caption">Inverso</Typography>}
                        />
                    </Stack>
                </AccordionDetails>
            </Accordion>

            <Divider />
            */}

            {/* Style Selector */}
            <Accordion disableGutters elevation={0} sx={{ background: 'transparent' }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="subtitle2">Vista</Typography>
                </AccordionSummary>
                <AccordionDetails sx={{ pt: 0 }}>
                    <FormControl size="small" fullWidth>
                        <InputLabel>Estilo</InputLabel>
                        <Select
                            value={selectedStyle}
                            label="Estilo"
                            onChange={(e) => setSelectedStyle(e.target.value)}
                        >
                            <MenuItem value="seismap_default" sx={{ display: 'none' }}>Por defecto</MenuItem>
                            <MenuItem value="seismap_circles_magnitude">Círculos — Magnitud</MenuItem>
                            <MenuItem value="seismap_circles_depth">Círculos — Profundidad</MenuItem>
                            <MenuItem value="seismap_circles_age">Círculos — Antigüedad</MenuItem>
                            <MenuItem value="seismap_points_magnitude">Puntos — Magnitud</MenuItem>
                            <MenuItem value="seismap_points_depth">Puntos — Profundidad</MenuItem>
                            <MenuItem value="seismap_points_age">Puntos — Antigüedad</MenuItem>
                        </Select>
                    </FormControl>
                </AccordionDetails>
            </Accordion>

            <Divider />

            <Box sx={{ p: 1.5 }}>
                <Button
                    fullWidth
                    variant={drawingMode ? 'outlined' : 'contained'}
                    color={drawingMode ? 'warning' : 'primary'}
                    size="small"
                    startIcon={loadingEvents ? <CircularProgress size={16} color="inherit" /> : drawingMode ? <CancelIcon /> : <PolylineIcon />}
                    onClick={onToggleDrawing}
                    disabled={loadingEvents}
                >
                    {drawingMode ? 'Cancelar selección' : 'Seleccionar área'}
                </Button>
            </Box>

        </Box>
    );
};

export default MapControlsPanel;
