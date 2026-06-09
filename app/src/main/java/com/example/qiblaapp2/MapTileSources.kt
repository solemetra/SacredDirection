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
}
