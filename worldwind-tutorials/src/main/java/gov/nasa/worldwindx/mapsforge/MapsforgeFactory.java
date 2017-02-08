package gov.nasa.worldwindx.mapsforge;

import android.os.Handler;
import android.os.Looper;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.layer.Layer;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.ogc.gpkg.GeoPackage;
import gov.nasa.worldwind.ogc.gpkg.GpkgContent;
import gov.nasa.worldwind.ogc.gpkg.GpkgSpatialReferenceSystem;
import gov.nasa.worldwind.ogc.gpkg.GpkgTileFactory;
import gov.nasa.worldwind.ogc.gpkg.GpkgTileMatrixSet;
import gov.nasa.worldwind.ogc.gpkg.GpkgTileUserMetrics;
import gov.nasa.worldwind.shape.TiledSurfaceImage;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.LevelSetConfig;
import gov.nasa.worldwind.util.Logger;

/**
 * Created by hydrologis on 08/02/17.
 */

public class MapsforgeFactory {

    protected Handler mainLoopHandler = new Handler(Looper.getMainLooper());


    public interface Callback {

        void creationSucceeded(MapsforgeFactory factory, Layer layer);

        void creationFailed(MapsforgeFactory factory, Layer layer, Throwable ex);
    }


    public Layer createFromMapsforge(String pathName, MapsforgeFactory.Callback callback) {
        if (pathName == null) {
            throw new IllegalArgumentException(
                    Logger.logMessage(Logger.ERROR, "LayerFactory", "createFromMapsforge", "missingPathName"));
        }

        if (callback == null) {
            throw new IllegalArgumentException(
                    Logger.logMessage(Logger.ERROR, "LayerFactory", "createFromMapsforge", "missingCallback"));
        }

        // Create a layer in which to asynchronously populate with renderables for the GeoPackage contents.
        RenderableLayer layer = new RenderableLayer();

        // Disable picking for the layer; terrain surface picking is performed automatically by WorldWindow.
        layer.setPickEnabled(false);

        MapsforgeAsyncTask task = new MapsforgeAsyncTask(this, pathName, layer, callback);

        try {
            WorldWind.taskService().execute(task);
        } catch (RejectedExecutionException logged) { // singleton task service is full; this should never happen but we check anyway
            callback.creationFailed(this, layer, logged);
        }

        return layer;
    }


    protected static class MapsforgeAsyncTask implements Runnable {

        protected MapsforgeFactory factory;

        protected String pathName;

        protected Layer layer;

        protected MapsforgeFactory.Callback callback;

        public MapsforgeAsyncTask(MapsforgeFactory factory, String pathName, Layer layer, MapsforgeFactory.Callback callback) {
            this.factory = factory;
            this.pathName = pathName;
            this.layer = layer;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                this.factory.createFromMapsforgeAsync(this.pathName, this.layer, this.callback);
            } catch (final Throwable ex) {
                this.factory.mainLoopHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.creationFailed(factory, layer, ex);
                    }
                });
            }
        }
    }

    protected void createFromMapsforgeAsync(String pathName, Layer layer, MapsforgeFactory.Callback callback) {

        File[] files = new File[]{new File(pathName)};
        Mapsforge mapsforge = new Mapsforge(InternalRenderTheme.OSMARENDER.name(), 3, 21, 256, files, null, MultiMapDataStore.DataPolicy.RETURN_ALL);
        BoundingBox bbox = mapsforge.getBoundingBox();

        final RenderableLayer mapsforgeRenderables = new RenderableLayer();

        LevelSetConfig config = new LevelSetConfig();
        config.sector.set(bbox.minLatitude, bbox.minLongitude,
                bbox.maxLatitude  - bbox.minLatitude, bbox.maxLongitude - bbox.minLongitude);
        config.firstLevelDelta = 180;
        config.numLevels = 21; // zero when there are no zoom levels, (0 = -1 + 1)
        config.tileWidth = 256;
        config.tileHeight = 256;

        TiledSurfaceImage surfaceImage = new TiledSurfaceImage();
        surfaceImage.setLevelSet(new LevelSet(config));
        surfaceImage.setTileFactory(new MapsforgeTileFactory(mapsforge));
        mapsforgeRenderables.addRenderable(surfaceImage);

        if (mapsforgeRenderables.count() == 0) {
            throw new RuntimeException(
                    Logger.makeMessage("LayerFactory", "createFromMapsforgeAsync", "Unsupported GeoPackage contents"));
        }

        final RenderableLayer finalLayer = (RenderableLayer) layer;
        final MapsforgeFactory.Callback finalCallback = callback;

        // Add the tiled surface image to the layer on the main thread and notify the caller. Request a redraw to ensure
        // that the image displays on all WorldWindows the layer may be attached to.
        this.mainLoopHandler.post(new Runnable() {
            @Override
            public void run() {
                finalLayer.addAllRenderables(mapsforgeRenderables);
                finalCallback.creationSucceeded(MapsforgeFactory.this, finalLayer);
                WorldWind.requestRedraw();
            }
        });
    }
}
