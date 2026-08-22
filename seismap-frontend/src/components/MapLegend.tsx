import React, { useState, useRef } from 'react';
import { Box, Paper, Typography, IconButton, Portal } from '@mui/material';
import Draggable from 'react-draggable';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';

interface MapLegendProps {
    styleName: string;
    /**
     * Renders into a document.body portal as a viewport-fixed element instead
     * of an absolutely-positioned child of the nearest positioned ancestor.
     * Use inside a Dialog/Modal so the legend isn't clipped to its container
     * and can be dragged anywhere on screen, above the modal backdrop.
     */
    fixed?: boolean;
}

/** Style labels for display in the legend title */
const STYLE_LABELS: Record<string, string> = {
    seismap_default: 'Por defecto',
    seismap_circles_magnitude: 'Círculos — Magnitud',
    seismap_circles_depth: 'Círculos — Profundidad',
    seismap_circles_age: 'Círculos — Antigüedad',
    seismap_points_magnitude: 'Puntos — Magnitud',
    seismap_points_depth: 'Puntos — Profundidad',
    seismap_points_age: 'Puntos — Antigüedad',
    seismap_circles_depth_profile: 'Profundidad',
    seismap_points_depth_profile: 'Profundidad',
};

const MapLegend: React.FC<MapLegendProps> = ({ styleName, fixed = false }) => {
    const [isExpanded, setIsExpanded] = useState(true);
    const nodeRef = useRef<HTMLDivElement>(null);

    if (styleName === 'seismap_default') return null;

    const legendUrl = `/api/maps/legend?name=${encodeURIComponent(styleName)}`;
    const label = STYLE_LABELS[styleName] ?? styleName;

    const content = (
        <Draggable nodeRef={nodeRef} handle=".drag-handle" bounds={fixed ? 'body' : 'parent'}>
            <div
                ref={nodeRef}
                style={
                    fixed
                        ? { position: 'fixed', top: 120, right: 32, zIndex: 1350 }
                        : { position: 'absolute', top: 100, right: 24, zIndex: 1000 }
                }
            >
                <Paper
                    elevation={6}
                    sx={{
                        bgcolor: 'rgba(18, 18, 30, 0.92)',
                        backdropFilter: 'blur(8px)',
                        borderRadius: 2,
                        minWidth: fixed ? 220 : 160,
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column'
                    }}
                >
                    {/* Header / Drag Handle */}
                    <Box
                        className="drag-handle"
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            p: 1,
                            bgcolor: 'rgba(255, 255, 255, 0.05)',
                            cursor: 'grab',
                            '&:active': { cursor: 'grabbing' },
                            borderBottom: isExpanded ? '1px solid rgba(255, 255, 255, 0.1)' : 'none'
                        }}
                    >
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <DragIndicatorIcon sx={{ color: 'grey.500', fontSize: 20, mr: 0.5 }} />
                            <Typography
                                variant="subtitle2"
                                sx={{ color: 'grey.100', fontWeight: 600, userSelect: 'none' }}
                            >
                                Leyenda: {label}
                            </Typography>
                        </Box>
                        <IconButton
                            size="small"
                            onClick={() => setIsExpanded(!isExpanded)}
                            sx={{ ml: 1, color: 'grey.400' }}
                        >
                            {isExpanded ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
                        </IconButton>
                    </Box>

                    {/* Collapsible Content */}
                    {isExpanded && (
                        <Box sx={{ p: 2, bgcolor: 'white', borderBottomLeftRadius: 8, borderBottomRightRadius: 8 }}>
                            <Box
                                component="img"
                                src={legendUrl}
                                alt="Leyenda del mapa"
                                sx={{
                                    display: 'block',
                                    maxWidth: '100%', // Allows it to be as wide as the legend needs
                                    width: 'auto',
                                    minWidth: fixed ? 260 : 200,   // Make it much larger
                                    height: 'auto',
                                    borderRadius: 1
                                }}
                                onError={(e: React.SyntheticEvent<HTMLImageElement>) => {
                                    e.currentTarget.style.display = 'none';
                                }}
                            />
                        </Box>
                    )}
                </Paper>
            </div>
        </Draggable>
    );

    return fixed ? <Portal>{content}</Portal> : content;
};

export default MapLegend;
