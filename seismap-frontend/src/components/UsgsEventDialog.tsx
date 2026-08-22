import React, { useCallback, useRef } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, Box, Grid, Link,
} from '@mui/material';
import PublicIcon from '@mui/icons-material/Public';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style';
import { toLonLat } from 'ol/proj';
import 'ol/ol.css';

export interface UsgsEventProperties {
    depth: number;
    date: string;
    magnitude: number | null;
    magnitude_type: string | null;
    place: string | null;
    url: string | null;
    /** Real-world location in EPSG:3857 (Web Mercator) — same projection OL maps use by default. */
    longitude: number;
    latitude: number;
}

interface Props {
    open: boolean;
    event: UsgsEventProperties | null;
    onClose: () => void;
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleString('es-AR', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
    });
}

const UsgsEventDialog: React.FC<Props> = ({ open, event, onClose }) => {
    const mapInstanceRef = useRef<Map | null>(null);
    const resizeObserverRef = useRef<ResizeObserver | null>(null);

    // A callback ref (rather than useRef + useEffect) ties map creation
    // directly to the container DOM node actually being attached, which
    // Dialog's mount/transition timing made unreliable for a plain effect —
    // the effect could run before the ref was set, and never re-ran since
    // the ref itself isn't a reactive dependency.
    const setMapContainer = useCallback((node: HTMLDivElement | null) => {
        resizeObserverRef.current?.disconnect();
        resizeObserverRef.current = null;
        if (mapInstanceRef.current) {
            mapInstanceRef.current.setTarget(undefined);
            mapInstanceRef.current = null;
        }
        if (!node || !event) return;

        const center: [number, number] = [event.longitude, event.latitude];
        const markerFeature = new Feature({ geometry: new Point(center) });
        markerFeature.setStyle(
            new Style({
                image: new CircleStyle({
                    radius: 8,
                    fill: new Fill({ color: 'rgba(171, 71, 188, 0.6)' }),
                    stroke: new Stroke({ color: '#6A1B9A', width: 2 }),
                }),
            })
        );

        const vectorLayer = new VectorLayer({ source: new VectorSource({ features: [markerFeature] }) });

        const map = new Map({
            target: node,
            layers: [new TileLayer({ source: new OSM() }), vectorLayer],
            view: new View({ center, zoom: 6 }),
        });
        mapInstanceRef.current = map;

        // Belt-and-suspenders: force a resize recompute if the container's
        // size settles after this point (e.g. the dialog is still animating).
        const resizeObserver = new ResizeObserver(() => map.updateSize());
        resizeObserver.observe(node);
        resizeObserverRef.current = resizeObserver;
    }, [event]);

    const [lonDeg, latDeg] = event ? toLonLat([event.longitude, event.latitude]) : [null, null];

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth
            PaperProps={{ sx: { bgcolor: 'background.paper' } }}>
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <PublicIcon color="secondary" />
                Sismo histórico (USGS)
            </DialogTitle>

            <DialogContent dividers>
                {event && (
                    <Grid container spacing={2}>
                        <Grid size={{ xs: 12 }}>
                            <Box
                                ref={setMapContainer}
                                sx={{
                                    width: '100%',
                                    height: 250,
                                    borderRadius: 1,
                                    overflow: 'hidden',
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    mb: 1,
                                }}
                            />
                        </Grid>
                        <Grid size={{ xs: 12 }}>
                            <Typography variant="subtitle2" color="text.secondary">Lugar</Typography>
                            <Typography variant="body1" gutterBottom>{event.place || '—'}</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Fecha y Hora (UTC)</Typography>
                            <Typography variant="body1" gutterBottom>{formatDate(event.date)}</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Profundidad</Typography>
                            <Typography variant="body1" gutterBottom>{event.depth.toFixed(1)} km</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Latitud</Typography>
                            <Typography variant="body1" gutterBottom>{latDeg?.toFixed(4) ?? '—'}</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Longitud</Typography>
                            <Typography variant="body1" gutterBottom>{lonDeg?.toFixed(4) ?? '—'}</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Magnitud</Typography>
                            <Typography variant="body1" gutterBottom>
                                {event.magnitude != null ? `${event.magnitude.toFixed(1)} ${event.magnitude_type || ''}` : '—'}
                            </Typography>
                        </Grid>
                        {event.url && (
                            <Grid size={{ xs: 12 }}>
                                <Box>
                                    <Link href={event.url} target="_blank" rel="noopener noreferrer">
                                        Ver en USGS Earthquake Catalog
                                    </Link>
                                </Box>
                            </Grid>
                        )}
                    </Grid>
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

export default UsgsEventDialog;
