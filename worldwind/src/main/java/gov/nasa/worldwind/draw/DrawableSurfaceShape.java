/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.draw;

import android.opengl.GLES20;

import java.util.ArrayList;

import gov.nasa.worldwind.geom.Matrix3;
import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.render.Framebuffer;
import gov.nasa.worldwind.render.Texture;
import gov.nasa.worldwind.util.Pool;

public class DrawableSurfaceShape implements Drawable {

    public DrawShapeState drawState = new DrawShapeState();

    public Sector sector = new Sector();

    private Matrix4 mvpMatrix = new Matrix4();

    private Matrix3 texCoordMatrix = new Matrix3();

    private Pool<DrawableSurfaceShape> pool;

    public DrawableSurfaceShape() {
    }

    public static DrawableSurfaceShape obtain(Pool<DrawableSurfaceShape> pool) {
        DrawableSurfaceShape instance = pool.acquire(); // get an instance from the pool
        return (instance != null) ? instance.setPool(pool) : new DrawableSurfaceShape().setPool(pool);
    }

    private DrawableSurfaceShape setPool(Pool<DrawableSurfaceShape> pool) {
        this.pool = pool;
        return this;
    }

    @Override
    public void recycle() {
        this.drawState.reset();

        if (this.pool != null) { // return this instance to the pool
            this.pool.release(this);
            this.pool = null;
        }
    }

    public void draw(DrawContext dc) {
        if (this.drawState.program == null || !this.drawState.program.useProgram(dc)) {
            return; // program unspecified or failed to build
        }

        // Accumulate shapes in the draw context's scratch list.
        // TODO accumulate in a geospatial quadtree
        ArrayList<Object> scratchList = dc.scratchList();

        try {
            // Add this shape.
            scratchList.add(this);

            // Add all shapes that are contiguous in the drawable queue.
            Drawable next;
            while ((next = dc.peekDrawable()) != null && next.getClass() == this.getClass()) { // check if the drawable at the front of the queue can be batched
                scratchList.add(dc.pollDrawable()); // take it off the queue
            }

            // Draw the accumulated shapes on each drawable terrain.
            for (int idx = 0, len = dc.getDrawableTerrainCount(); idx < len; idx++) {
                // Get the drawable terrain associated with the draw context.
                DrawableTerrain terrain = dc.getDrawableTerrain(idx);
                // Draw the accumulated surface shapes to a texture representing the terrain's sector.
                if (this.drawShapesToTexture(dc, terrain) > 0) {
                    // Draw the texture containing the rasterized shapes onto the terrain geometry.
                    this.drawTextureToTerrain(dc, terrain);
                }
            }
        } finally {
            // Clear the accumulated shapes.
            scratchList.clear();
        }
    }

    protected int drawShapesToTexture(DrawContext dc, DrawableTerrain terrain) {
        // Shapes have been accumulated in the draw context's scratch list.
        ArrayList<Object> scratchList = dc.scratchList();

        // The terrain's sector defines the geographic region in which to draw.
        Sector terrainSector = terrain.getSector();

        // Keep track of the number of shapes drawn into the texture.
        int shapeCount = 0;

        try {
            Framebuffer framebuffer = dc.surfaceFramebuffer();
            if (!framebuffer.bindFramebuffer(dc)) {
                return 0; // framebuffer failed to bind
            }

            // Clear the framebuffer.
            Texture colorAttachment = framebuffer.getAttachedTexture(GLES20.GL_COLOR_ATTACHMENT0);
            GLES20.glViewport(0, 0, colorAttachment.getWidth(), colorAttachment.getHeight());
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Use the draw context's pick mode.
            this.drawState.program.enablePickMode(dc.pickMode);

            // Disable texturing.
            this.drawState.program.enableTexture(false);

            // Transform geographic coordinates to texture fragments appropriate for the terrain sector.
            // TODO capture this in a method on Matrix4
            this.mvpMatrix.setToIdentity();
            this.mvpMatrix.multiplyByTranslation(-1, -1, 0);
            this.mvpMatrix.multiplyByScale(2 / terrainSector.deltaLongitude(), 2 / terrainSector.deltaLatitude(), 1);
            this.mvpMatrix.multiplyByTranslation(-terrainSector.minLongitude(), -terrainSector.minLatitude(), 0);
            this.drawState.program.loadModelviewProjection(this.mvpMatrix);

            for (int idx = 0, len = scratchList.size(); idx < len; idx++) {
                // Get the shape.
                DrawableSurfaceShape shape = (DrawableSurfaceShape) scratchList.get(idx);

                if (!shape.sector.intersects(terrainSector)) {
                    continue;
                }

                if (shape.drawState.vertexBuffer == null || !shape.drawState.vertexBuffer.bindBuffer(dc)) {
                    continue; // vertex buffer unspecified or failed to bind
                }

                if (shape.drawState.elementBuffer == null || !shape.drawState.elementBuffer.bindBuffer(dc)) {
                    continue; // element buffer unspecified or failed to bind
                }

                // Use the shape's vertex point attribute.
                GLES20.glVertexAttribPointer(0 /*vertexPoint*/, 2, GLES20.GL_FLOAT, false, 0, 0);

                // Draw the specified primitives to the framebuffer texture.
                for (int primIdx = 0; primIdx < shape.drawState.primCount; primIdx++) {
                    DrawShapeState.DrawElements prim = shape.drawState.prims[primIdx];
                    this.drawState.program.loadColor(prim.color);
                    GLES20.glLineWidth(prim.lineWidth);
                    GLES20.glDrawElements(prim.mode, prim.count, prim.type, prim.offset);
                }

                // Accumulate the number of shapes drawn into the texture.
                shapeCount++;
            }
        } finally {
            // Restore the default World Wind OpenGL state.
            dc.bindFramebuffer(0);
            GLES20.glViewport(dc.viewport.x, dc.viewport.y, dc.viewport.width, dc.viewport.height);
            GLES20.glLineWidth(1);
        }

        return shapeCount;
    }

    protected void drawTextureToTerrain(DrawContext dc, DrawableTerrain terrain) {
        if (!terrain.useVertexPointAttrib(dc, 0 /*vertexPoint*/)) {
            return; // terrain vertex attribute failed to bind
        }

        if (!terrain.useVertexTexCoordAttrib(dc, 1 /*vertexTexCoord*/)) {
            return; // terrain vertex attribute failed to bind
        }

        dc.activeTextureUnit(GLES20.GL_TEXTURE0);
        Texture colorAttachment = dc.surfaceFramebuffer().getAttachedTexture(GLES20.GL_COLOR_ATTACHMENT0);
        if (!colorAttachment.bindTexture(dc)) {
            return; // framebuffer texture failed to bind
        }

        // Configure the program to draw texture fragments unmodified and aligned with the terrain.
        // TODO consolidate pickMode and enableTexture into a single textureMode
        // TODO it's confusing that pickMode must be disabled during picking to correctly display the contents of the texture
        this.drawState.program.enablePickMode(false);
        this.drawState.program.enableTexture(true);
        this.drawState.program.loadTexCoordMatrix(this.texCoordMatrix);
        this.drawState.program.loadColor(1, 1, 1, 1);

        // Use the draw context's modelview projection matrix, transformed to terrain local coordinates.
        Vec3 terrainOrigin = terrain.getVertexOrigin();
        this.mvpMatrix.set(dc.modelviewProjection);
        this.mvpMatrix.multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
        this.drawState.program.loadModelviewProjection(this.mvpMatrix);

        // Set up to use vertex tex coord attributes.
        GLES20.glEnableVertexAttribArray(1 /*vertexTexCoord*/);

        // Draw the terrain as triangles.
        terrain.drawTriangles(dc);

        // Restore the default World Wind OpenGL state.
        GLES20.glDisableVertexAttribArray(1 /*vertexTexCoord*/);
    }
}
