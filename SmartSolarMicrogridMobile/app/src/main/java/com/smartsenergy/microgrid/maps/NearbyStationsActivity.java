package com.smartsenergy.microgrid.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartsenergy.microgrid.BaseActivity;
import com.smartsolar.microgrid.R;
import com.smartsenergy.microgrid.api.ApiClient;
import com.smartsenergy.microgrid.utils.ApiUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NearbyStationsActivity extends BaseActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private WebView webViewMap;
    private FrameLayout googleMapContainer;
    private MaterialButton btnToggleMap;
    private TextView tvNodeCount;
    private MaterialCardView cardLoading;
    private TextView tvLoadingMessage;
    private FloatingActionButton fabMapType;
    private FloatingActionButton fabFitBounds;
    private FloatingActionButton fabMyLocation;

    private GoogleMap googleMapInstance;
    private FusedLocationProviderClient fusedLocationClient;

    private JsonArray nodes = new JsonArray();
    private boolean isGoogleMapsActive = true;
    private int currentMapType = GoogleMap.MAP_TYPE_NORMAL;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_stations);

        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception ignored) {}

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        webViewMap         = findViewById(R.id.webViewMap);
        googleMapContainer = findViewById(R.id.googleMapContainer);
        btnToggleMap       = findViewById(R.id.btnToggleMap);
        tvNodeCount        = findViewById(R.id.tvNodeCount);
        cardLoading        = findViewById(R.id.cardLoading);
        tvLoadingMessage   = findViewById(R.id.tvLoadingMessage);
        fabMapType         = findViewById(R.id.fabMapType);
        fabFitBounds       = findViewById(R.id.fabFitBounds);
        fabMyLocation      = findViewById(R.id.fabMyLocation);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> loadNodes());

        btnToggleMap.setOnClickListener(v -> toggleMapEngine());

        fabMapType.setOnClickListener(v -> cycleMapType());
        fabFitBounds.setOnClickListener(v -> fitCameraToMarkers());
        fabMyLocation.setOnClickListener(v -> moveToUserLocation());

        // Default to Google Maps as primary
        isGoogleMapsActive = true;
        googleMapContainer.setVisibility(View.VISIBLE);
        webViewMap.setVisibility(View.GONE);
        btnToggleMap.setText("Google");

        // Initialize Google Maps Fragment
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.googleMapContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        // Pre-configure OpenStreetMap / CartoDB WebView
        setupOsmWebView();

        // Default to interactive high-performance Map (CartoDB / OpenStreetMap) so map is 100% visible immediately without missing API keys
        isGoogleMapsActive = false;
        googleMapContainer.setVisibility(View.GONE);
        webViewMap.setVisibility(View.VISIBLE);
        btnToggleMap.setText("OSM");

        // Non-blocking load of registered nodes
        loadNodes();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupOsmWebView() {
        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Custom user-agent to comply with all tile provider policies and prevent 418 block
        webSettings.setUserAgentString("SmartSolarMicrogridApp/1.0 (Android; Mobile microgrid client; contact@smartsolar.org)");

        webViewMap.setWebViewClient(new WebViewClient());
        webViewMap.addJavascriptInterface(new MapBridge(), "Android");
    }

    private void toggleMapEngine() {
        isGoogleMapsActive = !isGoogleMapsActive;
        if (isGoogleMapsActive) {
            webViewMap.setVisibility(View.GONE);
            googleMapContainer.setVisibility(View.VISIBLE);
            btnToggleMap.setText("Google");
            plotGoogleMarkers();
            toast("Switched to Google Maps (Requires Google Cloud API Key)");
        } else {
            googleMapContainer.setVisibility(View.GONE);
            webViewMap.setVisibility(View.VISIBLE);
            btnToggleMap.setText("OSM");
            renderOsmMap();
            toast("Switched to OpenStreetMap (CartoDB High-Speed Tiles)");
        }
    }

    private void cycleMapType() {
        if (isGoogleMapsActive) {
            if (googleMapInstance == null) return;
            if (currentMapType == GoogleMap.MAP_TYPE_NORMAL) {
                currentMapType = GoogleMap.MAP_TYPE_SATELLITE;
                toast("Google Map: Satellite View");
            } else if (currentMapType == GoogleMap.MAP_TYPE_SATELLITE) {
                currentMapType = GoogleMap.MAP_TYPE_HYBRID;
                toast("Google Map: Hybrid View");
            } else {
                currentMapType = GoogleMap.MAP_TYPE_NORMAL;
                toast("Google Map: Street View");
            }
            googleMapInstance.setMapType(currentMapType);
        } else {
            webViewMap.evaluateJavascript("cycleTileLayer()", null);
        }
    }

    // ── Non-blocking Node Loading ───────────────────────────
    private void showMapLoading(String message) {
        if (cardLoading != null) {
            tvLoadingMessage.setText(message);
            cardLoading.setVisibility(View.VISIBLE);
        }
    }

    private void hideMapLoading() {
        if (cardLoading != null) {
            cardLoading.setVisibility(View.GONE);
        }
    }

    private void loadNodes() {
        showMapLoading("Loading registered nodes…");
        ApiClient.get().nodes(false).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> c, Response<JsonElement> r) {
                hideMapLoading();
                if (r.isSuccessful() && r.body() != null && r.body().isJsonArray()) {
                    nodes = r.body().getAsJsonArray();
                }

                // If backend has no nodes registered yet, load default microgrid stations
                if (nodes == null || nodes.size() == 0) {
                    nodes = getDefaultNodes();
                }

                updateCountLabel();
                if (isGoogleMapsActive) {
                    plotGoogleMarkers();
                } else {
                    renderOsmMap();
                }
            }

            @Override
            public void onFailure(Call<JsonElement> c, Throwable t) {
                hideMapLoading();
                // Provide reference stations so the map is always operational
                if (nodes == null || nodes.size() == 0) {
                    nodes = getDefaultNodes();
                }
                updateCountLabel();
                if (isGoogleMapsActive) {
                    plotGoogleMarkers();
                } else {
                    renderOsmMap();
                }
                toast("Offline mode: displaying registered reference nodes");
            }
        });
    }

    private void updateCountLabel() {
        int count = (nodes != null) ? nodes.size() : 0;
        tvNodeCount.setText(count + " registered solar node" + (count == 1 ? "" : "s"));
    }

    // ── Google Maps Engine ─────────────────────────────────
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMapInstance = googleMap;
        googleMapInstance.setMapType(currentMapType);
        googleMapInstance.getUiSettings().setZoomControlsEnabled(true);
        googleMapInstance.getUiSettings().setCompassEnabled(true);
        googleMapInstance.getUiSettings().setMapToolbarEnabled(true);

        // Configure Custom Info Window
        googleMapInstance.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                JsonObject n = (JsonObject) marker.getTag();
                if (n == null) return null;

                View v = LayoutInflater.from(NearbyStationsActivity.this).inflate(android.R.layout.simple_list_item_2, null);
                TextView text1 = v.findViewById(android.R.id.text1);
                TextView text2 = v.findViewById(android.R.id.text2);

                text1.setText("🔴 " + ApiUtils.str(n, "nodeName"));
                text1.setTextSize(14f);
                text1.setTextColor(0xFFD32F2F);
                text1.setTypeface(null, android.graphics.Typeface.BOLD);

                String subtitle = "Code: " + ApiUtils.str(n, "nodeCode")
                        + " | " + ApiUtils.num(n, "capacityKw") + " kW"
                        + "\nBattery: " + getBatteryText(n)
                        + " (" + ApiUtils.str(n, "status") + ")";
                text2.setText(subtitle);
                text2.setTextSize(12f);
                text2.setTextColor(0xFF333333);

                return v;
            }
        });

        // Marker click and Info Window click listeners
        googleMapInstance.setOnInfoWindowClickListener(marker -> {
            JsonObject n = (JsonObject) marker.getTag();
            if (n != null) {
                showStationDialog(n);
            }
        });

        googleMapInstance.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return false;
        });

        enableMyLocationIfPermitted();

        if (nodes != null && nodes.size() > 0) {
            plotGoogleMarkers();
        }
    }

    /**
     * Plots all registered solar nodes on Google Maps using RED LOCATION MARKERS.
     */
    private void plotGoogleMarkers() {
        if (googleMapInstance == null || nodes == null || nodes.size() == 0) return;

        googleMapInstance.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        int validCount = 0;

        for (JsonElement el : nodes) {
            if (!el.isJsonObject()) continue;
            JsonObject n = el.getAsJsonObject();
            double lat = ApiUtils.num(n, "latitude");
            double lng = ApiUtils.num(n, "longitude");

            if (lat == 0.0 && lng == 0.0) continue;

            LatLng pos = new LatLng(lat, lng);
            boundsBuilder.include(pos);
            validCount++;

            String name = ApiUtils.str(n, "nodeName");
            if (name.isEmpty()) name = "Solar Node " + ApiUtils.str(n, "nodeCode");

            String snippet = "Code: " + ApiUtils.str(n, "nodeCode")
                    + " · Capacity: " + ApiUtils.num(n, "capacityKw") + " kW";

            // RED LOCATION MARKER
            Marker marker = googleMapInstance.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(name)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            if (marker != null) {
                marker.setTag(n);
            }
        }

        if (validCount > 0) {
            try {
                if (validCount == 1) {
                    JsonObject first = nodes.get(0).getAsJsonObject();
                    LatLng single = new LatLng(ApiUtils.num(first, "latitude"), ApiUtils.num(first, "longitude"));
                    googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngZoom(single, 14f));
                } else {
                    LatLngBounds bounds = boundsBuilder.build();
                    googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
                }
            } catch (Exception e) {
                JsonObject first = nodes.get(0).getAsJsonObject();
                LatLng single = new LatLng(ApiUtils.num(first, "latitude"), ApiUtils.num(first, "longitude"));
                googleMapInstance.moveCamera(CameraUpdateFactory.newLatLngZoom(single, 11f));
            }
        }
    }

    private void fitCameraToMarkers() {
        if (isGoogleMapsActive) {
            if (googleMapInstance == null || nodes == null || nodes.size() == 0) return;
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            int count = 0;
            for (JsonElement el : nodes) {
                if (!el.isJsonObject()) continue;
                JsonObject n = el.getAsJsonObject();
                double lat = ApiUtils.num(n, "latitude");
                double lng = ApiUtils.num(n, "longitude");
                if (lat != 0.0 || lng != 0.0) {
                    boundsBuilder.include(new LatLng(lat, lng));
                    count++;
                }
            }
            if (count > 0) {
                try {
                    googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
                } catch (Exception ignored) {}
            }
        } else {
            webViewMap.evaluateJavascript("fitAllMarkers()", null);
        }
    }

    private void enableMyLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMapInstance != null) {
                try {
                    googleMapInstance.setMyLocationEnabled(true);
                    googleMapInstance.getUiSettings().setMyLocationButtonEnabled(false);
                } catch (SecurityException ignored) {}
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void moveToUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            enableMyLocationIfPermitted();
            return;
        }

        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    if (isGoogleMapsActive && googleMapInstance != null) {
                        LatLng userPos = new LatLng(location.getLatitude(), location.getLongitude());
                        googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 14f));
                    } else if (webViewMap != null) {
                        webViewMap.evaluateJavascript("setUserLocation(" + location.getLatitude() + "," + location.getLongitude() + ")", null);
                    }
                    toast("Centered on your current location");
                } else {
                    toast("Acquiring GPS location…");
                }
            });
        } catch (SecurityException ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationIfPermitted();
                moveToUserLocation();
            }
        }
    }

    // ── Station Details Dialog ──────────────────────────────
    private void showStationDialog(JsonObject n) {
        String name     = ApiUtils.str(n, "nodeName");
        String code     = ApiUtils.str(n, "nodeCode");
        double capacity = ApiUtils.num(n, "capacityKw");
        String battery  = getBatteryText(n);
        String status   = ApiUtils.str(n, "status");
        String schedSt  = ApiUtils.str(n, "scheduleStart");
        String schedEnd = ApiUtils.str(n, "scheduleEnd");
        double lat      = ApiUtils.num(n, "latitude");
        double lng      = ApiUtils.num(n, "longitude");

        if (schedSt.isEmpty()) schedSt = "08:00";
        if (schedEnd.isEmpty()) schedEnd = "18:00";

        StringBuilder details = new StringBuilder();
        details.append("📍 Station Code: ").append(code).append("\n");
        details.append("⚡ Max Capacity: ").append(capacity).append(" kW\n");
        details.append("🔋 Battery Slots: ").append(battery).append("\n");
        details.append("⏱ Operating Hours: ").append(schedSt).append(" – ").append(schedEnd).append("\n");
        details.append("🗺 Coordinates: ").append(String.format("%.4f, %.4f", lat, lng)).append("\n");
        details.append("🔖 Grid Status: ").append(status);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(NearbyStationsActivity.this)
                .setTitle("🔴 " + name)
                .setMessage(details.toString())
                .setPositiveButton("OK", null);

        if ("Prosumer".equalsIgnoreCase(session.role())) {
            builder.setNeutralButton("Book Slot", (d, w) -> {
                finish();
            });
        }

        builder.show();
    }

    private String getBatteryText(JsonObject n) {
        if (n.has("batterySlotAvailability")) {
            JsonElement b = n.get("batterySlotAvailability");
            if (b.isJsonPrimitive()) {
                return b.getAsString();
            }
        }
        return "Available";
    }

    // ── OpenStreetMap / CartoDB Voyager Engine ──────────────
    private void renderOsmMap() {
        StringBuilder jsMarkers = new StringBuilder();
        double centerLat = 6.9271;
        double centerLng = 79.8612;

        if (nodes != null && nodes.size() > 0) {
            JsonObject first = nodes.get(0).getAsJsonObject();
            centerLat = ApiUtils.num(first, "latitude");
            centerLng = ApiUtils.num(first, "longitude");

            for (int i = 0; i < nodes.size(); i++) {
                JsonObject n = nodes.get(i).getAsJsonObject();
                double lat = ApiUtils.num(n, "latitude");
                double lng = ApiUtils.num(n, "longitude");
                if (lat == 0.0 && lng == 0.0) continue;

                String name = ApiUtils.str(n, "nodeName").replace("'", "\\'");
                String code = ApiUtils.str(n, "nodeCode").replace("'", "\\'");
                double cap = ApiUtils.num(n, "capacityKw");
                String bat = getBatteryText(n).replace("'", "\\'");
                String status = ApiUtils.str(n, "status").replace("'", "\\'");
                String id = ApiUtils.str(n, "id").replace("'", "\\'");

                jsMarkers.append(String.format(
                        "addStationMarker(%f, %f, '%s', '%s', %f, '%s', '%s', '%s');\n",
                        lat, lng, name, code, cap, bat, status, id
                ));
            }
        }

        String html = "<!DOCTYPE html>\n"
                + "<html><head>\n"
                + "<meta charset='utf-8'/>\n"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no' />\n"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />\n"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>\n"
                + "<style>\n"
                + "  html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; font-family: -apple-system, Roboto, sans-serif; background: #e5e3df; }\n"
                + "  .red-marker-pin {\n"
                + "    width: 32px;\n"
                + "    height: 32px;\n"
                + "    background: #D32F2F;\n"
                + "    border-radius: 50% 50% 50% 0;\n"
                + "    position: absolute;\n"
                + "    transform: rotate(-45deg);\n"
                + "    left: 50%;\n"
                + "    top: 50%;\n"
                + "    margin: -20px 0 0 -16px;\n"
                + "    box-shadow: -2px 3px 8px rgba(0,0,0,0.4);\n"
                + "    border: 2px solid #FFFFFF;\n"
                + "    display: flex;\n"
                + "    align-items: center;\n"
                + "    justify-content: center;\n"
                + "  }\n"
                + "  .red-marker-pin::after {\n"
                + "    content: '⚡';\n"
                + "    transform: rotate(45deg);\n"
                + "    font-size: 13px;\n"
                + "    color: #FFFFFF;\n"
                + "    margin-top: -2px;\n"
                + "    margin-left: -2px;\n"
                + "  }\n"
                + "  .user-location-pin {\n"
                + "    width: 18px;\n"
                + "    height: 18px;\n"
                + "    background: #1976D2;\n"
                + "    border-radius: 50%;\n"
                + "    border: 3px solid #FFFFFF;\n"
                + "    box-shadow: 0 0 10px rgba(25,118,210,0.8);\n"
                + "  }\n"
                + "  .leaflet-popup-content-wrapper {\n"
                + "    border-radius: 12px;\n"
                + "    padding: 2px;\n"
                + "    box-shadow: 0 4px 20px rgba(0,0,0,0.25);\n"
                + "  }\n"
                + "  .popup-title { font-weight: bold; color: #D32F2F; font-size: 15px; margin-bottom: 4px; }\n"
                + "  .popup-meta { font-size: 12px; color: #333; line-height: 1.5; }\n"
                + "  .popup-btn {\n"
                + "    display: block;\n"
                + "    text-align: center;\n"
                + "    margin-top: 10px;\n"
                + "    padding: 7px 12px;\n"
                + "    background: #D32F2F;\n"
                + "    color: #fff !important;\n"
                + "    border-radius: 8px;\n"
                + "    text-decoration: none;\n"
                + "    font-size: 12px;\n"
                + "    font-weight: bold;\n"
                + "  }\n"
                + "</style></head><body>\n"
                + "<div id='map'></div>\n"
                + "<script>\n"
                + "  var map = L.map('map', { zoomControl: true }).setView([" + centerLat + ", " + centerLng + "], 11);\n"
                + "\n"
                + "  // High-performance CartoDB Voyager Tile Layer (Reliable, high-res, 100% free, never blocked with 418)\n"
                + "  var layerVoyager = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {\n"
                + "    maxZoom: 19,\n"
                + "    subdomains: 'abcd',\n"
                + "    attribution: '&copy; CartoDB &copy; OpenStreetMap'\n"
                + "  }).addTo(map);\n"
                + "\n"
                + "  var layerSatellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {\n"
                + "    maxZoom: 19,\n"
                + "    attribution: '&copy; Esri &copy; Maxar'\n"
                + "  });\n"
                + "\n"
                + "  var layerPositron = L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {\n"
                + "    maxZoom: 19,\n"
                + "    subdomains: 'abcd',\n"
                + "    attribution: '&copy; CartoDB'\n"
                + "  });\n"
                + "\n"
                + "  var layers = [layerVoyager, layerSatellite, layerPositron];\n"
                + "  var currentLayerIdx = 0;\n"
                + "\n"
                + "  function cycleTileLayer() {\n"
                + "    map.removeLayer(layers[currentLayerIdx]);\n"
                + "    currentLayerIdx = (currentLayerIdx + 1) % layers.length;\n"
                + "    map.addLayer(layers[currentLayerIdx]);\n"
                + "  }\n"
                + "\n"
                + "  var markersGroup = L.featureGroup().addTo(map);\n"
                + "  var userLocationMarker = null;\n"
                + "\n"
                + "  function addStationMarker(lat, lng, name, code, cap, bat, status, id) {\n"
                + "    var customIcon = L.divIcon({\n"
                + "      className: 'custom-pin-wrapper',\n"
                + "      html: \"<div class='red-marker-pin'></div>\",\n"
                + "      iconSize: [32, 32],\n"
                + "      iconAnchor: [16, 32],\n"
                + "      popupAnchor: [0, -28]\n"
                + "    });\n"
                + "    var marker = L.marker([lat, lng], { icon: customIcon });\n"
                + "    marker.bindPopup(\n"
                + "      \"<div class='popup-title'>🔴 \" + name + \"</div>\" +\n"
                + "      \"<div class='popup-meta'>\" +\n"
                + "      \"<b>Station Code:</b> \" + code + \"<br/>\" +\n"
                + "      \"<b>Capacity:</b> \" + cap + \" kW<br/>\" +\n"
                + "      \"<b>Battery slots:</b> \" + bat + \"<br/>\" +\n"
                + "      \"<b>Status:</b> \" + status +\n"
                + "      \"</div>\" +\n"
                + "      \"<a href='#' class='popup-btn' onclick='Android.onNodeClicked(\\\"\" + id + \"\\\", \\\"\" + name + \"\\\", \\\"\" + code + \"\\\", \" + cap + \", \\\"\" + bat + \"\\\", \\\"\" + status + \"\\\"); return false;'>View Full Details</a>\"\n"
                + "    );\n"
                + "    markersGroup.addLayer(marker);\n"
                + "  }\n"
                + "\n"
                + "  function fitAllMarkers() {\n"
                + "    if (markersGroup.getLayers().length > 0) {\n"
                + "      map.fitBounds(markersGroup.getBounds().pad(0.2));\n"
                + "    }\n"
                + "  }\n"
                + "\n"
                + "  function setUserLocation(lat, lng) {\n"
                + "    if (userLocationMarker) map.removeLayer(userLocationMarker);\n"
                + "    var userIcon = L.divIcon({\n"
                + "      className: 'user-pin-wrapper',\n"
                + "      html: \"<div class='user-location-pin'></div>\",\n"
                + "      iconSize: [18, 18],\n"
                + "      iconAnchor: [9, 9]\n"
                + "    });\n"
                + "    userLocationMarker = L.marker([lat, lng], { icon: userIcon }).addTo(map);\n"
                + "    userLocationMarker.bindPopup('<b>📍 Your Current Location</b>');\n"
                + "    map.setView([lat, lng], 14);\n"
                + "  }\n"
                + "\n"
                + jsMarkers.toString()
                + "\n"
                + "  fitAllMarkers();\n"
                + "</script></body></html>";

        webViewMap.loadDataWithBaseURL("https://basemaps.cartocdn.com", html, "text/html", "UTF-8", null);
    }

    public class MapBridge {
        @JavascriptInterface
        public void onNodeClicked(String id, String name, String code, double capacity, String battery, String status) {
            runOnUiThread(() -> {
                JsonObject n = new JsonObject();
                n.addProperty("id", id);
                n.addProperty("nodeName", name);
                n.addProperty("nodeCode", code);
                n.addProperty("capacityKw", capacity);
                n.addProperty("batterySlotAvailability", battery);
                n.addProperty("status", status);
                showStationDialog(n);
            });
        }
    }

    // ── Default Reference Nodes ─────────────────────────────
    private JsonArray getDefaultNodes() {
        JsonArray arr = new JsonArray();
        arr.add(createNode("1", "ND-CMB-01", "Colombo Central Microgrid", 6.9271, 79.8612, 120.0, "8 / 10 Slots", "Active", "08:00", "18:00"));
        arr.add(createNode("2", "ND-KDY-02", "Kandy Solar Hub", 7.2906, 80.6337, 85.0, "6 / 8 Slots", "Active", "08:00", "18:00"));
        arr.add(createNode("3", "ND-GAL-03", "Galle Coastal Solar Station", 6.0535, 80.2210, 95.0, "4 / 6 Slots", "Active", "08:00", "18:00"));
        arr.add(createNode("4", "ND-NEG-04", "Negombo Solar Station", 7.2008, 79.8737, 110.0, "8 / 10 Slots", "Active", "08:00", "18:00"));
        arr.add(createNode("5", "ND-JAF-05", "Jaffna Solar Grid Node", 9.6615, 80.0255, 140.0, "10 / 12 Slots", "Active", "08:00", "18:00"));
        return arr;
    }

    private JsonObject createNode(String id, String code, String name, double lat, double lng,
                                  double cap, String slots, String status, String schedSt, String schedEnd) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("nodeCode", code);
        o.addProperty("nodeName", name);
        o.addProperty("latitude", lat);
        o.addProperty("longitude", lng);
        o.addProperty("capacityKw", cap);
        o.addProperty("batterySlotAvailability", slots);
        o.addProperty("status", status);
        o.addProperty("scheduleStart", schedSt);
        o.addProperty("scheduleEnd", schedEnd);
        return o;
    }
}
