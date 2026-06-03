package com.example.qiblaapp2

import android.content.Context
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.views.MapView

object MapStylePrefs {
    const val PREFS_NAME = "map_settings"
    const val STYLE_KEY = "map_style"
    const val STYLE_OSM = "osm"
    const val STYLE_MAPTILER = "maptiler"

    fun getStyle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(STYLE_KEY, STYLE_OSM) ?: STYLE_OSM
    }

    fun setStyle(context: Context, style: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(STYLE_KEY, style)
            .apply()
    }

    fun tileSourceFor(context: Context): ITileSource {
        return when (getStyle(context)) {
            STYLE_MAPTILER -> {
                if (BuildConfig.MAPTILER_API_KEY.isBlank()) {
                    MapTileSources.OSM_MAPNIK
                } else {
                    MapTileSources.mapTilerStreets(BuildConfig.MAPTILER_API_KEY)
                }
            }
            else -> MapTileSources.OSM_MAPNIK
        }
    }

    fun applyTo(mapView: MapView, context: Context): Boolean {
        val requestedMapTiler = getStyle(context) == STYLE_MAPTILER
        mapView.setTileSource(tileSourceFor(context))
        mapView.invalidate()
        return requestedMapTiler && BuildConfig.MAPTILER_API_KEY.isBlank()
    }
}
