import React, { useState } from 'react';
import {
    Box,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Typography,
    IconButton,
    Tooltip,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    Button,
    CircularProgress,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import SaveIcon from '@mui/icons-material/Save';
import { useMapStore } from '../store/mapStore';
import { mapService } from '../services/seismap';
import type { SeismapMap } from '../types/map';

const SavedMapsPanel: React.FC = () => {
    const { currentMap, savedMaps, setSavedMaps, setCurrentMap } = useMapStore();
    const [renameDialog, setRenameDialog] = useState<{ open: boolean; map: SeismapMap | null }>({
        open: false,
        map: null,
    });
    const [newName, setNewName] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSelect = async (map: SeismapMap) => {
        try {
            const full = await mapService.getById(map.id);
            setCurrentMap(full);
        } catch (err) {
            console.error('Error loading map', err);
        }
    };

    const handleSave = async () => {
        if (!currentMap) return;
        setLoading(true);
        try {
            await mapService.update(currentMap.id, currentMap);
            const maps = await mapService.listByUser();
            setSavedMaps(maps);
        } catch (err) {
            console.error('Error saving map', err);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await mapService.delete(id);
            const maps = await mapService.listByUser();
            setSavedMaps(maps);
        } catch (err) {
            console.error('Error deleting map', err);
        }
    };

    const handleRename = async () => {
        if (!renameDialog.map) return;
        try {
            await mapService.rename(renameDialog.map.id, newName);
            const maps = await mapService.listByUser();
            setSavedMaps(maps);
        } catch (err) {
            console.error('Error renaming map', err);
        } finally {
            setRenameDialog({ open: false, map: null });
        }
    };

    return (
        <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', px: 1, py: 0.5 }}>
                <Typography variant="overline" sx={{ flexGrow: 1, color: 'text.secondary' }}>
                    Mapas guardados
                </Typography>
                <Tooltip title="Guardar mapa actual">
                    <span>
                        <IconButton size="small" onClick={handleSave} disabled={!currentMap || loading}>
                            {loading ? <CircularProgress size={16} /> : <SaveIcon fontSize="small" />}
                        </IconButton>
                    </span>
                </Tooltip>
            </Box>

            <List dense disablePadding>
                {savedMaps.map((map) => (
                    <ListItem
                        key={map.id}
                        disablePadding
                        secondaryAction={
                            <Box>
                                <Tooltip title="Renombrar">
                                    <IconButton size="small" onClick={() => {
                                        setNewName(map.name);
                                        setRenameDialog({ open: true, map });
                                    }}>
                                        <EditIcon fontSize="small" />
                                    </IconButton>
                                </Tooltip>
                                <Tooltip title="Eliminar">
                                    <IconButton size="small" onClick={() => handleDelete(map.id)}>
                                        <DeleteIcon fontSize="small" />
                                    </IconButton>
                                </Tooltip>
                            </Box>
                        }
                    >
                        <ListItemButton
                            selected={currentMap?.id === map.id}
                            onClick={() => handleSelect(map)}
                            sx={{ pr: 8 }}
                        >
                            <ListItemText
                                primary={map.name}
                                primaryTypographyProps={{ variant: 'body2', noWrap: true }}
                            />
                        </ListItemButton>
                    </ListItem>
                ))}
                {savedMaps.length === 0 && (
                    <ListItem>
                        <ListItemText
                            secondary="No hay mapas guardados"
                            secondaryTypographyProps={{ variant: 'caption' }}
                        />
                    </ListItem>
                )}
            </List>

            {/* Rename Dialog */}
            <Dialog open={renameDialog.open} onClose={() => setRenameDialog({ open: false, map: null })}>
                <DialogTitle>Renombrar mapa</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        fullWidth
                        size="small"
                        label="Nuevo nombre"
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        sx={{ mt: 1 }}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setRenameDialog({ open: false, map: null })}>Cancelar</Button>
                    <Button variant="contained" onClick={handleRename}>Guardar</Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default SavedMapsPanel;
