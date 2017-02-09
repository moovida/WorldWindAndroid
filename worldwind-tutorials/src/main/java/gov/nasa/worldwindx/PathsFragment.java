/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx;

import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nasa.worldwind.BasicWorldWindowController;
import gov.nasa.worldwind.PickedObject;
import gov.nasa.worldwind.PickedObjectList;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.shape.Highlightable;
import gov.nasa.worldwind.shape.Path;
import gov.nasa.worldwind.shape.Placemark;
import gov.nasa.worldwind.shape.ShapeAttributes;
import gov.nasa.worldwind.util.Logger;

public class PathsFragment extends BasicGlobeFragment {

    private RenderableLayer pathsLayer;

    /**
     * Creates a new WorldWindow (GLSurfaceView) object with a set of Path shapes
     *
     * @return The WorldWindow object containing the globe.
     */
    @Override
    public WorldWindow createWorldWindow() {
        // Let the super class (BasicGlobeFragment) do the creation
        WorldWindow wwd = super.createWorldWindow();

        wwd.setWorldWindowController(new AddLinesController());

        // Create a layer to display the tutorial paths.
        pathsLayer = new RenderableLayer();
        wwd.getLayers().addLayer(pathsLayer);


        // Create a basic path with the default attributes, the default altitude mode (ABSOLUTE),
        // and the default path type (GREAT_CIRCLE).
        List<Position> positions = Arrays.asList(
                Position.fromDegrees(50, -180, 1e5),
                Position.fromDegrees(30, -100, 1e6),
                Position.fromDegrees(50, -40, 1e5)
        );
        Path path = new Path(positions);
        pathsLayer.addRenderable(path);

        // Create a terrain following path with the default attributes, and the default path type (GREAT_CIRCLE).
        positions = Arrays.asList(
                Position.fromDegrees(40, -180, 0),
                Position.fromDegrees(20, -100, 0),
                Position.fromDegrees(40, -40, 0)
        );
        path = new Path(positions);
        path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND); // clamp the path vertices to the ground
        path.setFollowTerrain(true); // follow the ground between path vertices
        pathsLayer.addRenderable(path);

        // Create an extruded path with the default attributes, the default altitude mode (ABSOLUTE),
        // and the default path type (GREAT_CIRCLE).
        positions = Arrays.asList(
                Position.fromDegrees(30, -180, 1e5),
                Position.fromDegrees(10, -100, 1e6),
                Position.fromDegrees(30, -40, 1e5)
        );
        path = new Path(positions);
        path.setExtrude(true); // extrude the path from the ground to each path position's altitude
        pathsLayer.addRenderable(path);

        // Create an extruded path with custom attributes that display the extruded vertical lines,
        // make the extruded interior 50% transparent, and increase the path line with.
        positions = Arrays.asList(
                Position.fromDegrees(20, -180, 1e5),
                Position.fromDegrees(0, -100, 1e6),
                Position.fromDegrees(20, -40, 1e5)
        );
        ShapeAttributes attrs = new ShapeAttributes();
        attrs.setDrawVerticals(true); // display the extruded verticals
        attrs.setInteriorColor(new Color(1, 1, 1, 0.5f)); // 50% transparent white
        attrs.setOutlineWidth(5);
        path = new Path(positions, attrs);
        path.setExtrude(true); // extrude the path from the ground to each path position's altitude
        pathsLayer.addRenderable(path);

        return wwd;
    }


    /**
     * This inner class is a custom WorldWindController that handles both picking and navigation via a combination of
     * the native World Wind navigation gestures and Android gestures. This class' onTouchEvent method arbitrates
     * between pick events and globe navigation events.
     */
    public class AddLinesController extends BasicWorldWindowController {
        private Path currentPath = null;
        private List<Position> pathPoints = new ArrayList<>();
        private List<Placemark> placemarks = new ArrayList<>();
        private final ShapeAttributes finalAttributes;
        private final ShapeAttributes createAttributes;


        public AddLinesController() {
            finalAttributes = new ShapeAttributes();
            finalAttributes.setOutlineColor(new Color(0, 0, 1, 1f));
            finalAttributes.setOutlineWidth(8);

            createAttributes = new ShapeAttributes();
            createAttributes.setOutlineColor(new Color(255 / 255f, 227 / 255f, 58 / 255f, 1f));
            createAttributes.setOutlineWidth(16);
        }


        /**
         * Assign a subclassed SimpleOnGestureListener to a GestureDetector to handle the "pick" events.
         */
        protected GestureDetector pickGestureDetector = new GestureDetector(
                getContext().getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return false;   // By not consuming this event, we allow it to pass on to the navigation gesture handlers
            }

            @Override
            public void onLongPress(MotionEvent e) {

                pathsLayer.removeAllRenderables(placemarks);
                currentPath.setAttributes(finalAttributes);
                currentPath = null;
                pathPoints = null;
                getWorldWindow().requestRedraw();

                Snackbar.make(getView(), "Line closed.", Snackbar.LENGTH_SHORT).show();

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (currentPath == null) {
                    currentPath = new Path(createAttributes);
                    currentPath.setAltitudeMode(WorldWind.CLAMP_TO_GROUND); // clamp the path vertices to the ground
                    currentPath.setFollowTerrain(true);

                    pathPoints = new ArrayList<>();

                    pathsLayer.addRenderable(currentPath);
                }

                float x = e.getX();
                float y = e.getY();

                Line pickRay = new Line();
                getWorldWindow().rayThroughScreenPoint(x, y, pickRay);

                Vec3 result = new Vec3();
                boolean intersect = getWorldWindow().getGlobe().intersect(pickRay, result);
                if (intersect) {

                    Position position = new Position();
                    getWorldWindow().getGlobe().cartesianToGeographic(result.x, result.y, result.z, position);

                    pathPoints.add(position);
                    currentPath.setPositions(pathPoints);

                    Placemark node = Placemark.createWithColorAndSize(position, createAttributes.getOutlineColor(), (int) (createAttributes.getOutlineWidth() * 2));
                    placemarks.add(node);
                    pathsLayer.addRenderable(node);

                    getWorldWindow().requestRedraw();

                    Log.i("PATHSFRAGMENT", "from " + x + "/" + y + "   --->    " + position.toString());
                }

                return true;
            }
        });

        /**
         * Delegates events to the pick handler or the native World Wind navigation handlers.
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // Allow pick listener to process the event first.
            boolean consumed = this.pickGestureDetector.onTouchEvent(event);

            // If event was not consumed by the pick operation, pass it on the globe navigation handlers
            if (!consumed) {
                // The super class performs the pan, tilt, rotate and zoom
                return super.onTouchEvent(event);
            }
            return consumed;
        }


    }
}
