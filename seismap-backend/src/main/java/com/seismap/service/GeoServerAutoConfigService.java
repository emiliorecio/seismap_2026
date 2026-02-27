package com.seismap.service;

import com.seismap.config.GeoServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Automatically configures GeoServer on application startup:
 * - Creates workspace (if not exists)
 * - Creates PostGIS datastore (if not exists)
 * - Publishes feature types / layers (if not exist)
 * - Uploads a default SLD style (if not exists)
 *
 * All operations are idempotent — safe to run on every startup.
 */
@Service
public class GeoServerAutoConfigService {

  private static final Logger log = LoggerFactory.getLogger(GeoServerAutoConfigService.class);

  private final GeoServerProperties props;
  private final DataSource dataSource;
  private final RestClient restClient;

  public GeoServerAutoConfigService(GeoServerProperties props, DataSource dataSource) {
    this.props = props;
    this.dataSource = dataSource;
    this.restClient = RestClient.builder()
        .baseUrl(props.getUrl() + "/rest")
        .defaultHeaders(headers -> headers.setBasicAuth(props.getAdminUser(), props.getAdminPassword()))
        .build();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void configure() {
    log.info("GeoServer auto-configuration starting → {}", props.getUrl());
    try {
      createWorkspace();
      createDatastore();
      publishLayer("eventandaveragemagnitudes_live", "location",
          "Eventos sísmicos con magnitudes promedio");
      publishLayer("eventandaveragemagnitudes_live", "depthlocation",
          "Eventos sísmicos — vista de profundidad");
      uploadDefaultStyle();
      uploadThemedStyles();
      log.info("GeoServer auto-configuration completed successfully");
    } catch (Exception e) {
      log.warn("GeoServer auto-configuration failed (GeoServer may not be running): {}", e.getMessage());
    }
  }

  // ─── Workspace ───────────────────────────────────────────────────────────────

  private void createWorkspace() {
    if (resourceExists("/workspaces/" + props.getWorkspace())) {
      log.debug("Workspace '{}' already exists", props.getWorkspace());
      return;
    }

    String json = """
        {"workspace": {"name": "%s"}}
        """.formatted(props.getWorkspace());

    restClient.post()
        .uri("/workspaces")
        .contentType(MediaType.APPLICATION_JSON)
        .body(json)
        .retrieve()
        .toBodilessEntity();

    log.info("Created GeoServer workspace: {}", props.getWorkspace());
  }

  // ─── PostGIS Datastore ───────────────────────────────────────────────────────

  private void createDatastore() {
    String uri = "/workspaces/" + props.getWorkspace() + "/datastores/" + props.getDatastoreName();
    if (resourceExists(uri)) {
      log.debug("Datastore '{}' already exists", props.getDatastoreName());
      return;
    }

    // Resolve actual DB connection parameters from the DataSource
    String dbHost = "postgres";
    String dbPort = "5432";
    String dbName = "seismap";
    String dbUser = "seismap";
    String dbPassword = "seismap";

    try (Connection conn = dataSource.getConnection()) {
      DatabaseMetaData meta = conn.getMetaData();
      String url = meta.getURL(); // jdbc:postgresql://host:port/db
      if (url != null && url.startsWith("jdbc:postgresql://")) {
        String rest = url.substring("jdbc:postgresql://".length());
        String[] parts = rest.split("/", 2);
        String[] hostPort = parts[0].split(":", 2);
        dbHost = hostPort[0];
        dbPort = hostPort.length > 1 ? hostPort[1] : "5432";
        dbName = parts.length > 1 ? parts[1].split("\\?")[0] : "seismap";
        dbUser = meta.getUserName();
      }
    } catch (Exception e) {
      log.warn("Could not resolve DB metadata, using defaults: {}", e.getMessage());
    }

    String json = """
        {
          "dataStore": {
            "name": "%s",
            "type": "PostGIS",
            "connectionParameters": {
              "entry": [
                {"@key": "host",     "$": "%s"},
                {"@key": "port",     "$": "%s"},
                {"@key": "database", "$": "%s"},
                {"@key": "user",     "$": "%s"},
                {"@key": "passwd",   "$": "%s"},
                {"@key": "dbtype",   "$": "postgis"},
                {"@key": "schema",   "$": "public"},
                {"@key": "Expose primary keys", "$": "true"}
              ]
            }
          }
        }
        """.formatted(props.getDatastoreName(), dbHost, dbPort, dbName, dbUser, dbPassword);

    restClient.post()
        .uri("/workspaces/" + props.getWorkspace() + "/datastores")
        .contentType(MediaType.APPLICATION_JSON)
        .body(json)
        .retrieve()
        .toBodilessEntity();

    log.info("Created GeoServer PostGIS datastore: {}", props.getDatastoreName());
  }

  // ─── Feature Type (Layer) ────────────────────────────────────────────────────

  private void publishLayer(String tableName, String geometryColumn, String title) {
    // Strip "_live" suffix for the exposed layer name to avoid breaking the
    // frontend
    String logicalTableName = tableName.endsWith("_live") ? tableName.substring(0, tableName.length() - 5) : tableName;
    String layerName = "location".equals(geometryColumn)
        ? logicalTableName
        : logicalTableName + "_" + geometryColumn;

    String uri = "/workspaces/" + props.getWorkspace()
        + "/datastores/" + props.getDatastoreName()
        + "/featuretypes/" + layerName;

    if (resourceExists(uri)) {
      log.debug("Layer '{}' already exists", layerName);
      return;
    }

    String json = """
        {
          "featureType": {
            "name": "%s",
            "nativeName": "%s",
            "title": "%s",
            "srs": "EPSG:900913",
            "nativeCRS": "EPSG:900913",
            "enabled": true
          }
        }
        """.formatted(layerName, tableName, title);

    restClient.post()
        .uri("/workspaces/" + props.getWorkspace()
            + "/datastores/" + props.getDatastoreName()
            + "/featuretypes")
        .contentType(MediaType.APPLICATION_JSON)
        .body(json)
        .retrieve()
        .toBodilessEntity();

    log.info("Published GeoServer layer: {}:{}", props.getWorkspace(), layerName);
  }

  // ─── Default SLD Style ───────────────────────────────────────────────────────

  private void uploadDefaultStyle() {
    String styleName = "seismap_default";
    if (resourceExists("/styles/" + styleName)) {
      log.debug("Style '{}' already exists", styleName);
      return;
    }

    // A simple blue circle SLD for initial visualization
    String sld = """
        <?xml version="1.0" encoding="UTF-8"?>
        <StyledLayerDescriptor version="1.0.0"
          xmlns="http://www.opengis.net/sld"
          xmlns:ogc="http://www.opengis.net/ogc"
          xmlns:xlink="http://www.w3.org/1999/xlink"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.opengis.net/sld
            http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
          <NamedLayer>
            <Name>seismap_default</Name>
            <UserStyle>
              <Title>Seismap Default</Title>
              <FeatureTypeStyle>
                <Rule>
                  <Title>Evento sísmico</Title>
                  <PointSymbolizer>
                    <Graphic>
                      <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                          <CssParameter name="fill">#4FC3F7</CssParameter>
                          <CssParameter name="fill-opacity">0.7</CssParameter>
                        </Fill>
                        <Stroke>
                          <CssParameter name="stroke">#0288D1</CssParameter>
                          <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                      </Mark>
                      <Size>
                        <ogc:Add>
                          <ogc:Mul>
                            <ogc:PropertyName>rankindex</ogc:PropertyName>
                            <ogc:Literal>16</ogc:Literal>
                          </ogc:Mul>
                          <ogc:Literal>4</ogc:Literal>
                        </ogc:Add>
                      </Size>
                    </Graphic>
                  </PointSymbolizer>
                </Rule>
              </FeatureTypeStyle>
            </UserStyle>
          </NamedLayer>
        </StyledLayerDescriptor>
        """;

    // First create the style entry
    String json = """
        {"style": {"name": "%s", "filename": "%s.sld"}}
        """.formatted(styleName, styleName);

    restClient.post()
        .uri("/styles")
        .contentType(MediaType.APPLICATION_JSON)
        .body(json)
        .retrieve()
        .toBodilessEntity();

    // Then upload the SLD body
    restClient.put()
        .uri("/styles/" + styleName)
        .contentType(MediaType.valueOf("application/vnd.ogc.sld+xml"))
        .body(sld)
        .retrieve()
        .toBodilessEntity();

    log.info("Uploaded default SLD style: {}", styleName);
  }

  // ─── Themed SLD Styles ───────────────────────────────────────────────────────

  private void uploadThemedStyles() {
    uploadSldStyle("seismap_circles_magnitude", "Círculos por magnitud",
        """
            <Rule>
              <Title>Magnitud</Title>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Fill>
                      <CssParameter name="fill">
                        <ogc:Function name="if_then_else">
                          <ogc:Function name="isNull"><ogc:PropertyName>rankindex</ogc:PropertyName></ogc:Function>
                          <ogc:Literal>#7F7F7F</ogc:Literal>
                          <ogc:Function name="categorize">
                            <ogc:PropertyName>rankindex</ogc:PropertyName>
                            <ogc:Literal>#00FF00</ogc:Literal>
                            <ogc:Literal>0.25</ogc:Literal>
                            <ogc:Literal>#7FFF00</ogc:Literal>
                            <ogc:Literal>0.50</ogc:Literal>
                            <ogc:Literal>#FFFF00</ogc:Literal>
                            <ogc:Literal>0.75</ogc:Literal>
                            <ogc:Literal>#FF7F00</ogc:Literal>
                            <ogc:Literal>1.00</ogc:Literal>
                            <ogc:Literal>#FF0000</ogc:Literal>
                          </ogc:Function>
                        </ogc:Function>
                      </CssParameter>
                      <CssParameter name="fill-opacity">0.7</CssParameter>
                    </Fill>
                    <Stroke><CssParameter name="stroke">#333333</CssParameter><CssParameter name="stroke-width">0.5</CssParameter></Stroke>
                  </Mark>
                  <Size>
                    <ogc:Add><ogc:Mul><ogc:PropertyName>rankindex</ogc:PropertyName><ogc:Literal>20</ogc:Literal></ogc:Mul><ogc:Literal>4</ogc:Literal></ogc:Add>
                  </Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            """);

    uploadSldStyle("seismap_circles_depth", "Círculos por profundidad",
        """
            <Rule><Title>0-30 km</Title>
              <ogc:Filter><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>30</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo></ogc:Filter>
              <PointSymbolizer><Geometry><ogc:PropertyName>depthlocation</ogc:PropertyName></Geometry><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#F44336</CssParameter><CssParameter name="fill-opacity">0.7</CssParameter></Fill><Stroke><CssParameter name="stroke">#B71C1C</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size><ogc:Add><ogc:Mul><ogc:PropertyName>rankindex</ogc:PropertyName><ogc:Literal>16</ogc:Literal></ogc:Mul><ogc:Literal>4</ogc:Literal></ogc:Add></Size></Graphic></PointSymbolizer>
            </Rule>
            <Rule><Title>30-70 km</Title>
              <ogc:Filter><ogc:And><ogc:PropertyIsGreaterThan><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>30</ogc:Literal></ogc:PropertyIsGreaterThan><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>70</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo></ogc:And></ogc:Filter>
              <PointSymbolizer><Geometry><ogc:PropertyName>depthlocation</ogc:PropertyName></Geometry><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#9C27B0</CssParameter><CssParameter name="fill-opacity">0.7</CssParameter></Fill><Stroke><CssParameter name="stroke">#6A1B9A</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size><ogc:Add><ogc:Mul><ogc:PropertyName>rankindex</ogc:PropertyName><ogc:Literal>16</ogc:Literal></ogc:Mul><ogc:Literal>4</ogc:Literal></ogc:Add></Size></Graphic></PointSymbolizer>
            </Rule>
            <Rule><Title>70-300 km</Title>
              <ogc:Filter><ogc:And><ogc:PropertyIsGreaterThan><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>70</ogc:Literal></ogc:PropertyIsGreaterThan><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>300</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo></ogc:And></ogc:Filter>
              <PointSymbolizer><Geometry><ogc:PropertyName>depthlocation</ogc:PropertyName></Geometry><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#FFEB3B</CssParameter><CssParameter name="fill-opacity">0.7</CssParameter></Fill><Stroke><CssParameter name="stroke">#F9A825</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size><ogc:Add><ogc:Mul><ogc:PropertyName>rankindex</ogc:PropertyName><ogc:Literal>16</ogc:Literal></ogc:Mul><ogc:Literal>4</ogc:Literal></ogc:Add></Size></Graphic></PointSymbolizer>
            </Rule>
            <Rule><Title>+300 km</Title>
              <ogc:Filter><ogc:PropertyIsGreaterThan><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>300</ogc:Literal></ogc:PropertyIsGreaterThan></ogc:Filter>
              <PointSymbolizer><Geometry><ogc:PropertyName>depthlocation</ogc:PropertyName></Geometry><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#2196F3</CssParameter><CssParameter name="fill-opacity">0.7</CssParameter></Fill><Stroke><CssParameter name="stroke">#1565C0</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size><ogc:Add><ogc:Mul><ogc:PropertyName>rankindex</ogc:PropertyName><ogc:Literal>16</ogc:Literal></ogc:Mul><ogc:Literal>4</ogc:Literal></ogc:Add></Size></Graphic></PointSymbolizer>
            </Rule>
            """);

    uploadSldStyle("seismap_circles_age", "Círculos por antigüedad",
        """
            <Rule>
              <ogc:Filter>
                <ogc:Not>
                  <ogc:PropertyIsNull>
                    <ogc:PropertyName>name</ogc:PropertyName>
                  </ogc:PropertyIsNull>
                </ogc:Not>
              </ogc:Filter>
              <TextSymbolizer>
                <Label>
                  <ogc:PropertyName>name</ogc:PropertyName>
                </Label>
                <Font>
                  <CssParameter name="font-family">Arial</CssParameter>
                  <CssParameter name="font-size">12</CssParameter>
                  <CssParameter name="font-style">normal</CssParameter>
                  <CssParameter name="font-weight">bold</CssParameter>
                </Font>
                <LabelPlacement>
                  <PointPlacement>
                    <AnchorPoint>
                      <AnchorPointX>0.5</AnchorPointX>
                      <AnchorPointY>2.0</AnchorPointY>
                    </AnchorPoint>
                    <Displacement>
                      <DisplacementX>0</DisplacementX>
                      <DisplacementY>
                        <ogc:Function name="if_then_else">
                          <ogc:Function name="isNull">
                            <ogc:PropertyName>rankindex</ogc:PropertyName>
                          </ogc:Function>
                          <ogc:Literal>-3</ogc:Literal>
                          <ogc:Function name="categorize">
                            <ogc:PropertyName>rankindex</ogc:PropertyName>
                            <ogc:Literal>-3</ogc:Literal>
                            <ogc:Literal>0.25</ogc:Literal>
                            <ogc:Literal>-4.5</ogc:Literal>
                            <ogc:Literal>0.50</ogc:Literal>
                            <ogc:Literal>-7.5</ogc:Literal>
                            <ogc:Literal>0.75</ogc:Literal>
                            <ogc:Literal>-13</ogc:Literal>
                            <ogc:Literal>1.00</ogc:Literal>
                            <ogc:Literal>-24</ogc:Literal>
                          </ogc:Function>
                        </ogc:Function>
                      </DisplacementY>
                    </Displacement>
                  </PointPlacement>
                </LabelPlacement>
                <Halo/>
                <Fill/>
              </TextSymbolizer>
            </Rule>
            <Rule>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Fill>
                      <CssParameter name="fill">
                        <ogc:Function name="categorize">
                          <ogc:PropertyName>age</ogc:PropertyName>
                          <ogc:Literal>#FF0000</ogc:Literal>
                          <ogc:Literal>3600</ogc:Literal>
                          <ogc:Literal>#FF00FF</ogc:Literal>
                          <ogc:Literal>86400</ogc:Literal>
                          <ogc:Literal>#FFFF00</ogc:Literal>
                          <ogc:Literal>604800</ogc:Literal>
                          <ogc:Literal>#0000FF</ogc:Literal>
                        </ogc:Function>
                      </CssParameter>
                      <CssParameter name="fill-opacity">0.75</CssParameter>
                    </Fill>
                    <Stroke/>
                  </Mark>
                  <Size>
                    <ogc:Function name="if_then_else">
                      <ogc:Function name="isNull"><ogc:PropertyName>rankindex</ogc:PropertyName></ogc:Function>
                      <ogc:Literal>6</ogc:Literal>
                      <ogc:Function name="categorize">
                        <ogc:PropertyName>rankindex</ogc:PropertyName>
                        <ogc:Literal>6</ogc:Literal>
                        <ogc:Literal>0.25</ogc:Literal>
                        <ogc:Literal>9</ogc:Literal>
                        <ogc:Literal>0.50</ogc:Literal>
                        <ogc:Literal>15</ogc:Literal>
                        <ogc:Literal>0.75</ogc:Literal>
                        <ogc:Literal>26</ogc:Literal>
                        <ogc:Literal>1.00</ogc:Literal>
                        <ogc:Literal>48</ogc:Literal>
                      </ogc:Function>
                    </ogc:Function>
                  </Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            <Rule>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Stroke/>
                  </Mark>
                  <Size>3</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            """);

    uploadSldStyle("seismap_points_magnitude", "Puntos por magnitud",
        """
            <Rule><Title>Magnitud</Title>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Fill>
                      <CssParameter name="fill">
                        <ogc:Function name="if_then_else">
                          <ogc:Function name="isNull"><ogc:PropertyName>rankindex</ogc:PropertyName></ogc:Function>
                          <ogc:Literal>#7F7F7F</ogc:Literal>
                          <ogc:Function name="categorize">
                            <ogc:PropertyName>rankindex</ogc:PropertyName>
                            <ogc:Literal>#00FF00</ogc:Literal>
                            <ogc:Literal>0.25</ogc:Literal>
                            <ogc:Literal>#7FFF00</ogc:Literal>
                            <ogc:Literal>0.50</ogc:Literal>
                            <ogc:Literal>#FFFF00</ogc:Literal>
                            <ogc:Literal>0.75</ogc:Literal>
                            <ogc:Literal>#FF7F00</ogc:Literal>
                            <ogc:Literal>1.00</ogc:Literal>
                            <ogc:Literal>#FF0000</ogc:Literal>
                          </ogc:Function>
                        </ogc:Function>
                      </CssParameter>
                      <CssParameter name="fill-opacity">0.8</CssParameter>
                    </Fill>
                    <Stroke><CssParameter name="stroke">#333333</CssParameter><CssParameter name="stroke-width">0.5</CssParameter></Stroke>
                  </Mark>
                  <Size>8</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            """);

    uploadSldStyle("seismap_points_depth", "Puntos por profundidad",
        """
            <Rule><Title>0-30 km</Title>
              <ogc:Filter><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>30</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo></ogc:Filter>
              <PointSymbolizer><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#F44336</CssParameter><CssParameter name="fill-opacity">0.8</CssParameter></Fill><Stroke><CssParameter name="stroke">#B71C1C</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size>8</Size></Graphic></PointSymbolizer>
            </Rule>
            <Rule><Title>30-70 km</Title>
              <ogc:Filter><ogc:And><ogc:PropertyIsGreaterThan><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>30</ogc:Literal></ogc:PropertyIsGreaterThan><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>70</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo></ogc:And></ogc:Filter>
              <PointSymbolizer><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#9C27B0</CssParameter><CssParameter name="fill-opacity">0.8</CssParameter></Fill><Stroke><CssParameter name="stroke">#6A1B9A</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size>8</Size></Graphic></PointSymbolizer>
            </Rule>
            <Rule><Title>70-300 km</Title>
              <ogc:Filter><ogc:And><ogc:PropertyIsGreaterThan><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>70</ogc:Literal></ogc:PropertyIsGreaterThan><ogc:PropertyIsLessThanOrEqualTo><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>300</ogc:Literal></ogc:PropertyIsLessThanOrEqualTo></ogc:And></ogc:Filter>
              <PointSymbolizer><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#FFEB3B</CssParameter><CssParameter name="fill-opacity">0.8</CssParameter></Fill><Stroke><CssParameter name="stroke">#F9A825</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size>8</Size></Graphic></PointSymbolizer>
            </Rule>
            <Rule><Title>+300 km</Title>
              <ogc:Filter><ogc:PropertyIsGreaterThan><ogc:PropertyName>depth</ogc:PropertyName><ogc:Literal>300</ogc:Literal></ogc:PropertyIsGreaterThan></ogc:Filter>
              <PointSymbolizer><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#2196F3</CssParameter><CssParameter name="fill-opacity">0.8</CssParameter></Fill><Stroke><CssParameter name="stroke">#1565C0</CssParameter><CssParameter name="stroke-width">1</CssParameter></Stroke></Mark><Size>8</Size></Graphic></PointSymbolizer>
            </Rule>
            """);

    uploadSldStyle("seismap_points_age", "Puntos por antigüedad",
        """
            <Rule>
              <ogc:Filter>
                <ogc:Not>
                  <ogc:PropertyIsNull>
                    <ogc:PropertyName>name</ogc:PropertyName>
                  </ogc:PropertyIsNull>
                </ogc:Not>
              </ogc:Filter>
              <TextSymbolizer>
                <Label>
                  <ogc:PropertyName>name</ogc:PropertyName>
                </Label>
                <Font>
                  <CssParameter name="font-family">Arial</CssParameter>
                  <CssParameter name="font-size">12</CssParameter>
                  <CssParameter name="font-style">normal</CssParameter>
                  <CssParameter name="font-weight">bold</CssParameter>
                </Font>
                <LabelPlacement>
                  <PointPlacement>
                    <AnchorPoint>
                      <AnchorPointX>0.5</AnchorPointX>
                      <AnchorPointY>2.0</AnchorPointY>
                    </AnchorPoint>
                    <Displacement>
                      <DisplacementX>0</DisplacementX>
                      <DisplacementY>-3</DisplacementY>
                    </Displacement>
                  </PointPlacement>
                </LabelPlacement>
                <Halo/>
                <Fill/>
              </TextSymbolizer>
            </Rule>
            <Rule>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Fill>
                      <CssParameter name="fill">
                        <ogc:Function name="categorize">
                          <ogc:PropertyName>age</ogc:PropertyName>
                          <ogc:Literal>#FF0000</ogc:Literal>
                          <ogc:Literal>3600</ogc:Literal>
                          <ogc:Literal>#FF00FF</ogc:Literal>
                          <ogc:Literal>86400</ogc:Literal>
                          <ogc:Literal>#FFFF00</ogc:Literal>
                          <ogc:Literal>604800</ogc:Literal>
                          <ogc:Literal>#0000FF</ogc:Literal>
                        </ogc:Function>
                      </CssParameter>
                      <CssParameter name="fill-opacity">0.75</CssParameter>
                    </Fill>
                    <Stroke/>
                  </Mark>
                  <Size>6</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            <Rule>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Stroke/>
                  </Mark>
                  <Size>3</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            """);
  }

  private void uploadSldStyle(String styleName, String title, String rulesBody) {
    boolean exists = resourceExists("/styles/" + styleName);

    String sld = """
        <?xml version="1.0" encoding="UTF-8"?>
        <StyledLayerDescriptor version="1.0.0"
          xmlns="http://www.opengis.net/sld"
          xmlns:ogc="http://www.opengis.net/ogc"
          xmlns:xlink="http://www.w3.org/1999/xlink"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.opengis.net/sld
            http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
          <NamedLayer>
            <Name>%s</Name>
            <UserStyle>
              <Title>%s</Title>
              <FeatureTypeStyle>
                %s
              </FeatureTypeStyle>
            </UserStyle>
          </NamedLayer>
        </StyledLayerDescriptor>
        """.formatted(styleName, title, rulesBody);

    if (!exists) {
      String json = """
          {"style": {"name": "%s", "filename": "%s.sld"}}
          """.formatted(styleName, styleName);

      restClient.post()
          .uri("/styles")
          .contentType(MediaType.APPLICATION_JSON)
          .body(json)
          .retrieve()
          .toBodilessEntity();
    }

    restClient.put()
        .uri("/styles/" + styleName)
        .contentType(MediaType.valueOf("application/vnd.ogc.sld+xml"))
        .body(sld)
        .retrieve()
        .toBodilessEntity();

    log.info("Uploaded/Updated SLD style: {}", styleName);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private boolean resourceExists(String uri) {
    try {
      restClient.get()
          .uri(uri)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
            throw new RuntimeException("Not found");
          })
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
