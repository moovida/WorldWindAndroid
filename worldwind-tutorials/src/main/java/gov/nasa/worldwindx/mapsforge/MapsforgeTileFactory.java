/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx.mapsforge;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.gpkg.GeoPackage;
import gov.nasa.worldwind.ogc.gpkg.GpkgBitmapFactory;
import gov.nasa.worldwind.ogc.gpkg.GpkgTileMatrix;
import gov.nasa.worldwind.ogc.gpkg.GpkgTileUserMetrics;
import gov.nasa.worldwind.render.ImageSource;
import gov.nasa.worldwind.render.ImageTile;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileFactory;

public class MapsforgeTileFactory implements TileFactory {

    protected Mapsforge mapsforge;

    public MapsforgeTileFactory(Mapsforge mapsforge) {
        if (mapsforge == null) {
            throw new IllegalArgumentException(
                    Logger.logMessage(Logger.ERROR, "MapsforgeTileFactory", "constructor", "missingTiles"));
        }

        this.mapsforge = mapsforge;
    }

    @Override
    public Tile createTile(Sector sector, Level level, int row, int column) {
        if (sector == null) {
            throw new IllegalArgumentException(
                    Logger.logMessage(Logger.ERROR, "MapsforgeTileFactory", "createTile", "missingSector"));
        }

        if (level == null) {
            throw new IllegalArgumentException(
                    Logger.logMessage(Logger.ERROR, "MapsforgeTileFactory", "createTile", "missingLevel"));
        }

        ImageTile tile = new ImageTile(sector, level, row, column);

        int zoomLevel = level.levelNumber;

        double north = sector.maxLatitude();
        double south = sector.minLatitude();
        double east = sector.maxLongitude();
        double west = sector.minLongitude();
        double centerX = west + (east - west) / 2.0;
        double centerY = south + (north - south) / 2.0;

        int[] tileNumber = getTileNumber(centerY, centerX, zoomLevel);
        int x = tileNumber[0];
        int y = tileNumber[1];

        // Configure the tile with a bitmap factory that reads directly from the GeoPackage.
        ImageSource.BitmapFactory bitmapFactory = new MapsforgeBitmapFactory(this.mapsforge, zoomLevel, x, y);
        tile.setImageSource(ImageSource.fromBitmapFactory(bitmapFactory));

        return tile;
    }

    public static int[] getTileNumber(final double lat, final double lon, final int zoom) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor(
                (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return new int[]{xtile, ytile};
    }
}
