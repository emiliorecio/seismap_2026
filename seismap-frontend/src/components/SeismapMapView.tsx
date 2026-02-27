import React, { useEffect, useRef } from 'react';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import ImageLayer from 'ol/layer/Image';
import VectorLayer from 'ol/layer/Vector';
import OSM from 'ol/source/OSM';
import ImageWMS from 'ol/source/ImageWMS';
import VectorSource from 'ol/source/Vector';
import Draw from 'ol/interaction/Draw';
import { fromLonLat } from 'ol/proj';
import { Style, Fill, Stroke } from 'ol/style';
import Feature from 'ol/Feature';
import type { Geometry } from 'ol/geom';
import 'ol/ol.css';
import type { SeismapMap } from '../types/map';
import { buildCqlFilter } from '../utils/cqlFilter';

interface SeismapMapViewProps {
    centerLon?: number;
    centerLat?: number;
    zoom?: number;
    currentMap?: SeismapMap | null;
    styleName?: string;
    drawingMode?: boolean;
    onPolygonComplete?: (wkt: string) => void;
    onClearPolygon?: (clear: () => void) => void;
    onPointClick?: (eventId: number) => void;
}

const GEOSERVER_WMS_URL = '/geoserver/seismap/wms';
const LAYER_NAME = 'seismap:eventandaveragemagnitudes';

const POLYGON_STYLE = new Style({
    fill: new Fill({ color: 'rgba(33, 150, 243, 0.15)' }),
    stroke: new Stroke({ color: '#2196F3', width: 2, lineDash: [6, 3] }),
});

const SeismapMapView: React.FC<SeismapMapViewProps> = ({
    centerLon = -65,
    centerLat = -32,
    zoom = 5,
    currentMap = null,
    styleName = 'seismap_default',
    drawingMode = false,
    onPolygonComplete,
    onClearPolygon,
    onPointClick,
}) => {
    const mapRef = useRef<HTMLDivElement>(null);
    const olMapRef = useRef<Map | null>(null);
    const wmsLayerRef = useRef<ImageLayer<ImageWMS> | null>(null);
    const drawRef = useRef<Draw | null>(null);
    const vectorSourceRef = useRef<VectorSource<Feature<Geometry>>>(new VectorSource());

    // ── Initialize map ─────────────────────────────────────────────
    useEffect(() => {
        if (!mapRef.current) return;

        const wmsSource = new ImageWMS({
            url: GEOSERVER_WMS_URL,
            params: {
                LAYERS: LAYER_NAME,
                SRS: 'EPSG:900913',
                STYLES: styleName,
                CQL_FILTER: currentMap ? buildCqlFilter(currentMap) || undefined : undefined,
            },
            serverType: 'geoserver',
            ratio: 1,
        });

        const wmsLayer = new ImageLayer({ source: wmsSource, opacity: 0.85 });
        wmsLayerRef.current = wmsLayer;

        const vectorLayer = new VectorLayer({
            source: vectorSourceRef.current,
            style: POLYGON_STYLE,
        });

        const map = new Map({
            target: mapRef.current,
            layers: [
                new TileLayer({ source: new OSM() }),
                wmsLayer,
                vectorLayer,
            ],
            view: new View({
                center: fromLonLat([centerLon, centerLat]),
                zoom,
            }),
        });

        olMapRef.current = map;

        // Expose clear function to parent
        onClearPolygon?.(() => vectorSourceRef.current.clear());

        // Map click handler for GetFeatureInfo
        map.on('singleclick', (evt) => {
            // If we are drawing, don't trigger layer info click
            if (drawRef.current) return;

            const viewResolution = map.getView().getResolution();
            if (!viewResolution) return;

            const wmsSource = wmsLayerRef.current?.getSource();
            if (!wmsSource) return;

            const url = wmsSource.getFeatureInfoUrl(
                evt.coordinate,
                viewResolution,
                'EPSG:900913',
                { 'INFO_FORMAT': 'application/json' }
            );

            if (url) {
                fetch(url)
                    .then((response) => response.json())
                    .then((data) => {
                        if (data.features && data.features.length > 0) {
                            // GeoServer JSON returns properties.id (assuming the view has it)
                            const eventId = data.features[0].properties.id;
                            if (eventId && onPointClick) {
                                onPointClick(eventId);
                            }
                        }
                    })
                    .catch((err) => console.error('Failed to get feature info', err));
            }
        });

        return () => {
            map.setTarget(undefined);
            olMapRef.current = null;
            wmsLayerRef.current = null;
        };
    }, []);

    // ── Update WMS params on filter/style change ───────────────────
    useEffect(() => {
        const source = wmsLayerRef.current?.getSource();
        if (!source) return;
        const cql = currentMap ? buildCqlFilter(currentMap) : '';
        source.updateParams({ STYLES: styleName, CQL_FILTER: cql || undefined });
    }, [
        currentMap?.minDateType, currentMap?.minDate,
        currentMap?.minDateRelativeAmount, currentMap?.minDateRelativeUnits,
        currentMap?.maxDateType, currentMap?.maxDate,
        currentMap?.maxDateRelativeAmount, currentMap?.maxDateRelativeUnits,
        currentMap?.minDepthType, currentMap?.minDepth,
        currentMap?.maxDepthType, currentMap?.maxDepth,
        currentMap?.minMagnitudeType, currentMap?.minMagnitude,
        currentMap?.maxMagnitudeType, currentMap?.maxMagnitude,
        styleName,
    ]);

    // ── Toggle Draw interaction ────────────────────────────────────
    useEffect(() => {
        const map = olMapRef.current;
        if (!map) return;

        // Remove existing draw
        if (drawRef.current) {
            map.removeInteraction(drawRef.current);
            drawRef.current = null;
        }

        if (!drawingMode) return;

        // Clear previous polygon before starting a new one
        vectorSourceRef.current.clear();

        const draw = new Draw({
            source: vectorSourceRef.current,
            type: 'Polygon',
        });

        draw.on('drawend', (evt) => {
            const geom = evt.feature.getGeometry() as any;
            const coords: number[][] = geom.getCoordinates()[0];
            // Coordinates are already in EPSG:3857 = EPSG:900913 — use directly
            const wktCoords = coords.map((c: number[]) => `${c[0]} ${c[1]}`).join(', ');
            const wkt = `POLYGON((${wktCoords}))`;
            onPolygonComplete?.(wkt);
            // Deactivate draw after first polygon
            map.removeInteraction(draw);
            drawRef.current = null;
        });

        map.addInteraction(draw);
        drawRef.current = draw;

        return () => {
            map.removeInteraction(draw);
            drawRef.current = null;
        };
    }, [drawingMode]);

    return (
        <div
            ref={mapRef}
            style={{
                width: '100%',
                height: '100%',
                position: 'relative',
                cursor: drawingMode ? 'crosshair' : 'default',
            }}
        />
    );
};

export default SeismapMapView;
