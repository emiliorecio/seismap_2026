import React, { useEffect, useState } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, CircularProgress, Box, Grid,
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper
} from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';
import { eventService } from '../services/seismap';
import { toLonLat, fromLonLat } from 'ol/proj';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import { Style, Circle as CircleStyle, Fill, Stroke } from 'ol/style';

interface Magnitude {
    id: number;
    type: string;
    value: number;
    reportingAgency: { id: number; code: string };
}

interface EventDetail {
    id: number;
    date: string;
    depth: number;
    latitude: number;
    longitude: number;
    name?: string;
    notes?: string;
    reference?: string;
    perceivedDistance?: number;
    damagedDistance?: number;
    magnitudes: Magnitude[];
}

interface Props {
    open: boolean;
    eventId: number | null;
    onClose: () => void;
}

function formatDate(iso: string) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-AR', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
}

const EventDialog: React.FC<Props> = ({ open, eventId, onClose }) => {
    const [event, setEvent] = useState<EventDetail | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const mapRef = React.useRef<HTMLDivElement>(null);
    const olMapRef = React.useRef<Map | null>(null);

    useEffect(() => {
        if (open && eventId) {
            setLoading(true);
            setError('');
            eventService.getById(eventId)
                .then(data => {
                    // Extract lat/lon from the returned location point geometry
                    let epsgLon = data.longitude;
                    let epsgLat = data.latitude;

                    if (data.location && data.location.coordinates) {
                        epsgLon = data.location.coordinates[0];
                        epsgLat = data.location.coordinates[1];
                    }

                    // Convert from Web Mercator to typical Lat/Lon degrees
                    const [lonDeg, latDeg] = toLonLat([epsgLon, epsgLat], 'EPSG:3857');

                    setEvent({ ...data, latitude: latDeg, longitude: lonDeg });
                })
                .catch(err => {
                    console.error('Failed to load event details', err);
                    setError('Error al cargar los detalles del evento.');
                })
                .finally(() => setLoading(false));

        } else {
            setEvent(null);
            if (olMapRef.current) {
                olMapRef.current.setTarget(undefined);
                olMapRef.current = null;
            }
        }

        return () => {
            if (olMapRef.current) {
                olMapRef.current.setTarget(undefined);
                olMapRef.current = null;
            }
        };
    }, [open, eventId]);

    useEffect(() => {
        if (event && mapRef.current && !olMapRef.current) {
            const centerPoint = fromLonLat([event.longitude, event.latitude]);

            const markerFeature = new Feature({
                geometry: new Point(centerPoint),
            });

            markerFeature.setStyle(
                new Style({
                    image: new CircleStyle({
                        radius: 8,
                        fill: new Fill({ color: 'rgba(255, 0, 0, 0.6)' }),
                        stroke: new Stroke({ color: 'red', width: 2 }),
                    }),
                })
            );

            const vectorSource = new VectorSource({ features: [markerFeature] });
            const vectorLayer = new VectorLayer({ source: vectorSource });

            olMapRef.current = new Map({
                target: mapRef.current,
                layers: [
                    new TileLayer({ source: new OSM() }),
                    vectorLayer,
                ],
                view: new View({
                    center: centerPoint,
                    zoom: 6,
                }),
            });
        }
    }, [event]);

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth
            PaperProps={{ sx: { bgcolor: 'background.paper' } }}>
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <InfoIcon color="primary" />
                Detalles del Evento Sísmico
            </DialogTitle>

            <DialogContent dividers>
                {loading && (
                    <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                        <CircularProgress />
                    </Box>
                )}
                {error && (
                    <Typography color="error" align="center" sx={{ p: 2 }}>
                        {error}
                    </Typography>
                )}
                {event && !loading && (
                    <Grid container spacing={2}>
                        <Grid size={{ xs: 12 }}>
                            <Box
                                ref={mapRef}
                                sx={{
                                    width: '100%',
                                    height: 250,
                                    borderRadius: 1,
                                    overflow: 'hidden',
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    mb: 1
                                }}
                            />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Fecha y Hora</Typography>
                            <Typography variant="body1" gutterBottom>{formatDate(event.date)}</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Profundidad</Typography>
                            <Typography variant="body1" gutterBottom>{event.depth.toFixed(1)} km</Typography>
                        </Grid>

                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Latitud</Typography>
                            <Typography variant="body1" gutterBottom>{event.latitude?.toFixed(4) || '—'}</Typography>
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6 }}>
                            <Typography variant="subtitle2" color="text.secondary">Longitud</Typography>
                            <Typography variant="body1" gutterBottom>{event.longitude?.toFixed(4) || '—'}</Typography>
                        </Grid>

                        {(event.name || event.reference) && (
                            <Grid size={{ xs: 12 }}>
                                {event.name && (
                                    <>
                                        <Typography variant="subtitle2" color="text.secondary">Nombre</Typography>
                                        <Typography variant="body1" gutterBottom>{event.name}</Typography>
                                    </>
                                )}
                                {event.reference && (
                                    <>
                                        <Typography variant="subtitle2" color="text.secondary">Referencia</Typography>
                                        <Typography variant="body1" gutterBottom>{event.reference}</Typography>
                                    </>
                                )}
                            </Grid>
                        )}

                        {event.notes && (
                            <Grid size={{ xs: 12 }}>
                                <Typography variant="subtitle2" color="text.secondary">Notas</Typography>
                                <Typography variant="body2" sx={{ bgcolor: 'action.hover', p: 1, borderRadius: 1 }}>
                                    {event.notes}
                                </Typography>
                            </Grid>
                        )}

                        <Grid size={{ xs: 12 }} mt={2}>
                            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                                Magnitudes ({event.magnitudes?.length || 0})
                            </Typography>
                            {event.magnitudes && event.magnitudes.length > 0 ? (
                                <TableContainer component={Paper} variant="outlined">
                                    <Table size="small">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell>Tipo</TableCell>
                                                <TableCell align="right">Valor</TableCell>
                                                <TableCell>Agencia</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {event.magnitudes.map(mag => (
                                                <TableRow key={mag.id}>
                                                    <TableCell>{mag.type}</TableCell>
                                                    <TableCell align="right">{mag.value.toFixed(1)}</TableCell>
                                                    <TableCell>{mag.reportingAgency?.code || '—'}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            ) : (
                                <Typography variant="body2" color="text.secondary">
                                    No hay magnitudes registradas.
                                </Typography>
                            )}
                        </Grid>
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

export default EventDialog;
