import React from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogActions,
    Button, Typography, Box, Grid, Link,
} from '@mui/material';
import PublicIcon from '@mui/icons-material/Public';

export interface UsgsEventProperties {
    depth: number;
    date: string;
    magnitude: number | null;
    magnitude_type: string | null;
    place: string | null;
    url: string | null;
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
