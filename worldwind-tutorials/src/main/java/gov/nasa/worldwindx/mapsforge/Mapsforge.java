package gov.nasa.worldwindx.mapsforge;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.graphics.AndroidTileBitmap;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.labels.TileBasedLabelStore;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.ReadBuffer;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

import java.io.File;

/**
 * Created by hydrologis on 08/02/17.
 */
public class Mapsforge {
    // Reasonable defaults ..
    public static int MIN_ZOOM = 3;
    public static int MAX_ZOOM = 20;
    public static final int TILE_SIZE_PIXELS = 256;
    private final DisplayModel model = new DisplayModel();
    private final float scale = DisplayModel.getDefaultUserScaleFactor();
    private RenderThemeFuture theme = null;
    private XmlRenderTheme mXmlRenderTheme = null;
    private DatabaseRenderer renderer;

    private MultiMapDataStore mapDatabase;

    public Mapsforge(String cacheTileSourceName, int minZoom, int maxZoom, int tileSizePixels, File[] file, XmlRenderTheme xmlRenderTheme, MultiMapDataStore.DataPolicy dataPolicy) {
        mapDatabase = new MultiMapDataStore(dataPolicy);
        for (int i = 0; i < file.length; i++)
            mapDatabase.addMapDataStore(new MapFile(file[i]), false, false);

        if (AndroidGraphicFactory.INSTANCE == null) {
            throw new RuntimeException("Must call MapsForgeTileSource.createInstance(context.getApplication()); once before MapsForgeTileSource.createFromFiles().");
        }

        // mapsforge0.7.0
        InMemoryTileCache tileCache = new InMemoryTileCache(2);
        renderer = new DatabaseRenderer(mapDatabase, AndroidGraphicFactory.INSTANCE, tileCache,
                new TileBasedLabelStore(tileCache.getCapacityFirstLevel()), true, true);

        minZoom = MIN_ZOOM;
        maxZoom = renderer.getZoomLevelMax();

        Log.d("MAPSFORGE", "min=" + minZoom + " max=" + maxZoom + " tilesize=" + tileSizePixels);

        if (xmlRenderTheme == null)
            xmlRenderTheme = InternalRenderTheme.OSMARENDER;
        //we the passed in theme is different that the existing one, or the theme is currently null, create it
        if (xmlRenderTheme != mXmlRenderTheme || theme == null) {
            theme = new RenderThemeFuture(AndroidGraphicFactory.INSTANCE, xmlRenderTheme, model);
            //super important!! without the following line, all rendering activities will block until the theme is created.
            new Thread(theme).start();
        }
    }


    //The synchronized here is VERY important.  If missing, the mapDatabase read gets corrupted by multiple threads reading the file at once.
    public synchronized Bitmap renderTile(int x, int y, int zoom, int tileSize) {

        Tile tile = new Tile(x, y, (byte) zoom, tileSize);
        model.setFixedTileSize(tileSize);

        //You could try something like this to load a custom theme
        //try{
        //	jobTheme = new ExternalRenderTheme(themeFile);
        //}
        //catch(Exception e){
        //	jobTheme = InternalRenderTheme.OSMARENDER;
        //}


        if (mapDatabase == null)
            return null;
        try {
            //Draw the tile
            RendererJob mapGeneratorJob = new RendererJob(tile, mapDatabase, theme, model, scale, false, false);
            AndroidTileBitmap bmp = (AndroidTileBitmap) renderer.executeJob(mapGeneratorJob);
            if (bmp != null)
                return AndroidGraphicFactory.getBitmap(bmp);
        } catch (Exception ex) {
            Log.d("MAPSFORGE", "###################### Mapsforge tile generation failed", ex);
        }
        //Make the bad tile easy to spot
        Bitmap bitmap = Bitmap.createBitmap(TILE_SIZE_PIXELS, TILE_SIZE_PIXELS, Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.YELLOW);
        return bitmap;
    }


    public double[] getEWSNCenterXY() {
        BoundingBox boundingBox = mapDatabase.boundingBox();

        double minX = boundingBox.minLongitude;
        double maxX = boundingBox.maxLongitude;
        double minY = boundingBox.minLongitude;
        double maxY = boundingBox.maxLongitude;

        LatLong centerPoint = boundingBox.getCenterPoint();

        return new double[]{minX, maxX, minY, maxY, centerPoint.longitude, centerPoint.latitude};
    }

    public BoundingBox getBoundingBox() {
        return mapDatabase.boundingBox();
    }


    public static void createInstance(Application app) {
        if (AndroidGraphicFactory.INSTANCE == null) {
            AndroidGraphicFactory.createInstance(app);

            // see https://github.com/mapsforge/mapsforge/issues/868
            ReadBuffer.setMaximumBufferSize(6500000);
        }
    }


}
