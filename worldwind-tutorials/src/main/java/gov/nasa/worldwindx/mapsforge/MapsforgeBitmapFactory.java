/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx.mapsforge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import gov.nasa.worldwind.ogc.gpkg.GeoPackage;
import gov.nasa.worldwind.ogc.gpkg.GpkgTileUserData;
import gov.nasa.worldwind.render.ImageSource;
import gov.nasa.worldwind.util.Logger;

public class MapsforgeBitmapFactory implements ImageSource.BitmapFactory {

    protected Mapsforge mapsforge;

    protected int zoomLevel;

    protected int tileColumn;

    protected int tileRow;

    public MapsforgeBitmapFactory(Mapsforge mapsforge, int zoomLevel, int tileColumn, int tileRow) {
        if (mapsforge == null) {
            throw new IllegalArgumentException(
                    Logger.logMessage(Logger.ERROR, "MapsforgeBitmapFactory", "constructor", "missingTiles"));
        }

        this.mapsforge = mapsforge;
        this.zoomLevel = zoomLevel;
        this.tileColumn = tileColumn;
        this.tileRow = tileRow;
    }

    @Override
    public Bitmap createBitmap() {
        Bitmap bitmap = mapsforge.renderTile(tileColumn, tileRow, zoomLevel, 256);
        return bitmap;
    }
}
