/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx.mapsforge;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

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

        int zoomLevel = level.levelNumber + 3;

        int mapsforgeRow = (int) Math.pow(2, zoomLevel) - row - 1;


        double s = sector.minLatitude();
        double n = sector.maxLatitude();
        double e = sector.maxLongitude();
        double w = sector.minLongitude();
        double centerX = w + (e - w) / 2.0;
        double centerY = s + (n - s) / 2.0;

        int[] tileNumber = getTileNumber(centerY, centerX, zoomLevel);
        int x = tileNumber[0];
        int y = tileNumber[1];

        int[] tileNumberLL = getTileNumber(w, s, zoomLevel);
        int[] tileNumberUR = getTileNumber(e, n, zoomLevel);

        if (zoomLevel == 12) {
            int xNum = (tileNumberUR[0] - tileNumberLL[0]) + 1;
            int yNum = (tileNumberLL[1] - tileNumberUR[1]) + 1;
            Log.i("MAPSFORGETILEFACTORY", "TILES: " + xNum + " x " + yNum);
            Log.i("MAPSFORGETILEFACTORY", "POLYGON ((" + w + " " + s + ", " + w + " " + n + ", " + e + " " + n + ", " + e + " " + s + ", " + w + " " + s + "))");
            Log.i("MAPSFORGETILEFACTORY", "Z=" + zoomLevel + " x=" + x + " y=" + y + " mfrow=" + mapsforgeRow + " col=" + column + " row=" + row);
        }


//        double requestedCenterLon = centerX;
//        double requestedCenterLat = centerY;
//        double requestedMinX = w;
//        double requestedMaxX = e;
//        double requestedMinY = s;
//        double requestedMaxY = n;
//        double requestedWidth = requestedMaxX - requestedMinX;
//        double requestedHeight = requestedMaxY - requestedMinY;
//
//        int[] tileLL = getTileNumber(requestedMinY, requestedMinX, zoomLevel);
//        int[] tileUR = getTileNumber(requestedMaxY, requestedMaxX, zoomLevel);
//        int originalStartXTile = tileLL[0];
//        int originalStartYTile = tileUR[1];
//        int originalEndXTile = tileUR[0];
//        int originalEndYTile = tileLL[1];
//
//        int bufferTilesToAdd = 0;
//        int startXTile = originalStartXTile - bufferTilesToAdd;
//        int startYTile = originalStartYTile - bufferTilesToAdd;
//        int endXTile = originalEndXTile + bufferTilesToAdd;
//        int endYTile = originalEndYTile + bufferTilesToAdd;
//
//        // check the drawn region
//        double[] startNSWE = tile2boundingBox(startXTile, startYTile, zoomLevel);
//        double[] endNSWE = tile2boundingBox(endXTile, endYTile, zoomLevel);
//        double drawnMinX = Math.min(startNSWE[2], endNSWE[2]);
//        double drawnMaxX = Math.max(startNSWE[3], endNSWE[3]);
//        double drawnMinY = Math.min(startNSWE[1], endNSWE[1]);
//        double drawnMaxY = Math.max(startNSWE[0], endNSWE[0]);
//        double drawnWidth = drawnMaxX - drawnMinX;
//        double drawnHeight = drawnMaxY - drawnMinY;
//
//        double centerPercentX = (requestedCenterLon - drawnMinX) / drawnWidth;
//        double centerPercentY = (drawnMaxY - requestedCenterLat) / drawnHeight;
//
//        double scalefactorX = requestedWidth / drawnWidth;
//        double scalefactorY = requestedHeight / drawnHeight;
//
//        double tileDeltaMinX = requestedMinX - drawnMinX;
//        double tileDeltaMaxY = drawnMaxY - requestedMaxY;
//
//        int tileSize = mapsforge.getTileSizePixels();
//        int yTiles = endYTile - startYTile + 1;
//        int xTiles = endXTile - startXTile + 1;
//        int drawnImageWidth = xTiles * tileSize;
//        int drawnImageHeight = yTiles * tileSize;
//
//        int requestedImageWidth = (int) (drawnImageWidth * scalefactorX);
//        int requestedImageHeight = (int) (drawnImageHeight * scalefactorY);
//        int requestedImageShiftX = (int) (drawnImageWidth * tileDeltaMinX / drawnWidth);
//        int requestedImageShiftY = (int) (drawnImageHeight * tileDeltaMaxY / drawnHeight);
//
//        Bitmap bitmap = null;
//        try {
//            Log.i("MAPSFORGETILEFACTORY", "NEW BITMAP SIZE: " + drawnImageWidth + " x " + drawnImageHeight);
//            bitmap = Bitmap.createBitmap(drawnImageWidth, drawnImageHeight, Bitmap.Config.RGB_565);
//        } catch (Exception e1) {
//            e1.printStackTrace();
//
//        }
//        Canvas canvas = new Canvas(bitmap);
//
//
//        float c = 0;
//        float r = 0;
//        int firstY = 0;
//        for (int xTile = startXTile; xTile <= endXTile; xTile++) {
//            for (int yTile = startYTile; yTile <= endYTile; yTile++) {
//                Bitmap tmpBitmap = mapsforge.renderTile(xTile, yTile, zoomLevel);
//                canvas.drawBitmap(tmpBitmap, c * tileSize, r * tileSize, null);
//                tmpBitmap.recycle();
//                tmpBitmap = null;
//                r = r + tileSize;
//            }
//
//            c = c + tileSize;
//            r = firstY;
//        }
//
//        canvas.setBitmap(null);
//        try {
//            Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, tileSize, tileSize);
////            Bitmap finalBitmap = Bitmap.createBitmap(bitmap, requestedImageShiftX, requestedImageShiftY, requestedImageWidth, requestedImageHeight);
//
//            tile.setImageSource(ImageSource.fromBitmap(finalBitmap));
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }


//        int[] tileNumber2 = getTileNumber2(centerY, centerX, zoomLevel);
//        int x2 = tileNumber2[0];
//        int y2 = tileNumber2[1];

        // Configure the tile with a bitmap factory that reads directly from the GeoPackage.
        ImageSource.BitmapFactory bitmapFactory = new MapsforgeBitmapFactory(this.mapsforge, zoomLevel, x, y);
        tile.setImageSource(ImageSource.fromBitmapFactory(bitmapFactory));

        return tile;
    }

    public static int[] getTileNumber2(final double lat, final double lon, final int zoom) {
        double lat_rad = Math.toRadians(lat);
        double n = Math.pow(2.0, zoom);
        int xtile = (int) ((lon + 180.0) / 360.0 * n);
        int ytile = (int) ((1.0 - Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI) / 2.0 * n);
        return new int[]{xtile, ytile};
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

    public static double[] tile2boundingBox(final int x, final int y, final int zoom) {
        double[] nswe = new double[4];
        nswe[0] = tile2lat(y, zoom);
        nswe[1] = tile2lat(y + 1, zoom);
        nswe[2] = tile2lon(x, zoom);
        nswe[3] = tile2lon(x + 1, zoom);
        return nswe;
    }

    private static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
}
