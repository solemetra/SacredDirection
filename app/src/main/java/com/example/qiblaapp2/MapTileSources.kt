package com.example.qiblaapp2

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource

object MapTileSources {

    val OSM_MAPNIK: OnlineTileSourceBase = XYTileSource(
        "OSMMapnik",
        0,
        19,
        256,
        ".png",
        arrayOf("https://tile.openstreetmap.org/"),
        "© OpenStreetMap contributors"
    )

    val CARTO_VOYAGER: OnlineTileSourceBase = XYTileSource(
        "CartoVoyager",
        0,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        ),
        "© OpenStreetMap contributors © CARTO"
    )

    fun mapTilerStreets(apiKey: String): OnlineTileSourceBase {
        return XYTileSource(
            "MapTilerStreets",
            0,
            20,
            256,
            ".png?key=$apiKey",
            arrayOf("https://api.maptiler.com/maps/streets-v2/"),
            "© OpenStreetMap contributors © MapTiler"
        )
    }
}
