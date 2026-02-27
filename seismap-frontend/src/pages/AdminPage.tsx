import React, { useState, useEffect } from 'react';
import {
    Box,
    Typography,
    Button,
    List,
    ListItem,
    ListItemText,
    ListItemSecondaryAction,
    IconButton,
    Chip,
    Alert,
    CircularProgress,
    Paper,
    AppBar,
    Toolbar,
} from '@mui/material';
import { Link } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import { adminService } from '../services/seismap';

interface DataFile {
    name: string;
    size: string;
}

const AdminPage: React.FC = () => {
    const [files, setFiles] = useState<DataFile[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadingFile, setLoadingFile] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    useEffect(() => {
        adminService
            .listDataFiles()
            .then(setFiles)
            .catch(() => setError('Error al listar archivos'))
            .finally(() => setLoading(false));
    }, []);

    const handleLoad = async (filename: string) => {
        setLoadingFile(filename);
        setError(null);
        setSuccess(null);
        try {
            await adminService.loadDataFile(filename);
            setSuccess(`Archivo "${filename}" cargado correctamente.`);
        } catch (err: any) {
            setError(err?.response?.data?.message ?? `Error cargando "${filename}"`);
        } finally {
            setLoadingFile(null);
        }
    };

    const formatSize = (bytes: string) => {
        const b = parseInt(bytes);
        if (b < 1024) return `${b} B`;
        if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`;
        return `${(b / (1024 * 1024)).toFixed(1)} MB`;
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <AppBar position="static">
                <Toolbar variant="dense">
                    <IconButton color="inherit" component={Link} to="/" edge="start" sx={{ mr: 1 }}>
                        <ArrowBackIcon />
                    </IconButton>
                    <Typography variant="h6">Consola de Administración</Typography>
                </Toolbar>
            </AppBar>

            <Box sx={{ p: 3, maxWidth: 700 }}>
                <Typography variant="h6" gutterBottom>
                    Archivos de datos sísmicos
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Cargá archivos <code>.data</code> desde el directorio configurado en el servidor.
                </Typography>

                {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}
                {success && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>{success}</Alert>}

                {loading ? (
                    <CircularProgress />
                ) : files.length === 0 ? (
                    <Paper variant="outlined" sx={{ p: 3, textAlign: 'center' }}>
                        <Typography color="text.secondary">
                            No se encontraron archivos .data en el servidor.
                        </Typography>
                    </Paper>
                ) : (
                    <Paper variant="outlined">
                        <List dense disablePadding>
                            {files.map((file, i) => (
                                <ListItem key={file.name} divider={i < files.length - 1}>
                                    <ListItemText
                                        primary={file.name}
                                        secondary={formatSize(file.size)}
                                    />
                                    <ListItemSecondaryAction>
                                        <Chip
                                            label=".data"
                                            size="small"
                                            sx={{ mr: 1, fontSize: '0.65rem' }}
                                        />
                                        <Button
                                            size="small"
                                            variant="outlined"
                                            startIcon={
                                                loadingFile === file.name
                                                    ? <CircularProgress size={14} />
                                                    : <CloudUploadIcon />
                                            }
                                            disabled={!!loadingFile}
                                            onClick={() => handleLoad(file.name)}
                                        >
                                            Cargar
                                        </Button>
                                    </ListItemSecondaryAction>
                                </ListItem>
                            ))}
                        </List>
                    </Paper>
                )}
            </Box>
        </Box>
    );
};

export default AdminPage;
