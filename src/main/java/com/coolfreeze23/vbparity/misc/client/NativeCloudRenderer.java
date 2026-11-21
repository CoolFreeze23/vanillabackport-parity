package com.coolfreeze23.vbparity.misc.client;

import com.coolfreeze23.vbparity.VBParity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Port of the native 26.2 CloudRenderer for 1.21.1 (VB's "extended cloud reach" option): clouds are one cell per
 * opaque pixel of {@code clouds.png} (12x4x12 blocks), built in rings outwards from the camera inside a circle of
 * {@code Cloud Distance * 16} blocks, with the native face rules (top or bottom depending on whether the camera is
 * above, inside or below the layer, sides only towards the camera, inside faces in the 3x3 cells around the camera,
 * a single bottom face per cell for Fast clouds), native face shades times the cloud colour (alpha 0.8, a uniform like native), the native
 * drift ({@code gameTime % (width * 400) * 0.03} blocks per tick, z offset 3.96) and an alpha fade to the cloud fog
 * end. The mesh is built on the CPU (native builds it on the GPU from a face buffer) and rebuilt on the same events.
 */
public final class NativeCloudRenderer extends SimplePreparableReloadListener<Optional<NativeCloudRenderer.TextureData>> {
    public static final NativeCloudRenderer INSTANCE = new NativeCloudRenderer();

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final float CELL_SIZE = 12.0F;
    private static final float CELL_HEIGHT = 4.0F;

    // Native rendertype_clouds.vsh vertex table (per Direction 3D data value) and face shades.
    private static final float[][][] VERTICES = {
            {{1, 0, 0}, {1, 0, 1}, {0, 0, 1}, {0, 0, 0}},
            {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}},
            {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}},
            {{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}},
            {{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}},
            {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}};
    private static final float[] SHADES = {0.7F, 1.0F, 0.8F, 0.8F, 0.9F, 0.9F};

    public record TextureData(long[] cells, int width, int height) {}

    private enum RelativeCameraPos { ABOVE_CLOUDS, INSIDE_CLOUDS, BELOW_CLOUDS }

    @Nullable
    private TextureData texture;
    @Nullable
    private ShaderInstance shader;
    @Nullable
    private VertexBuffer buffer;
    private boolean needsRebuild = true;
    private boolean empty = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private int prevRadius = -1;
    private RelativeCameraPos prevRelativePos = RelativeCameraPos.INSIDE_CLOUDS;
    @Nullable
    private CloudStatus prevStatus;

    private NativeCloudRenderer() {}

    public void setShader(ShaderInstance shader) {
        this.shader = shader;
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    @Override
    protected Optional<TextureData> prepare(ResourceManager manager, ProfilerFiller profiler) {
        try (InputStream input = manager.open(TEXTURE_LOCATION); NativeImage image = NativeImage.read(input)) {
            int width = image.getWidth();
            int height = image.getHeight();
            long[] cells = new long[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = image.getPixelRGBA(x, y);
                    if (isCellEmpty(color)) {
                        cells[x + y * width] = 0L;
                    } else {
                        boolean north = isCellEmpty(image.getPixelRGBA(x, Math.floorMod(y - 1, height)));
                        boolean east = isCellEmpty(image.getPixelRGBA(Math.floorMod(x + 1, height), y));
                        boolean south = isCellEmpty(image.getPixelRGBA(x, Math.floorMod(y + 1, height)));
                        boolean west = isCellEmpty(image.getPixelRGBA(Math.floorMod(x - 1, height), y));
                        cells[x + y * width] = (long) color << 4 | (north ? 1 : 0) << 3 | (east ? 1 : 0) << 2 | (south ? 1 : 0) << 1 | (west ? 1 : 0);
                    }
                }
            }
            return Optional.of(new TextureData(cells, width, height));
        } catch (IOException e) {
            VBParity.LOG.error("Failed to load cloud texture", e);
            return Optional.empty();
        }
    }

    @Override
    protected void apply(Optional<TextureData> data, ResourceManager manager, ProfilerFiller profiler) {
        this.texture = data.orElse(null);
        this.needsRebuild = true;
    }

    /** Native {@code ARGB.alpha(color) < 10} (NativeImage pixels are ABGR; the alpha byte is the same). */
    private static boolean isCellEmpty(int color) {
        return (color >>> 24 & 0xFF) < 10;
    }

    /**
     * Renders the clouds natively. Returns false when it cannot (no texture or shader yet), so the caller can fall
     * back to the 1.21.1 clouds.
     */
    public boolean render(Matrix4f modelView, Matrix4f projection, float partialTick, double camX, double camY, double camZ, int range) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (this.texture == null || this.shader == null || level == null) {
            return false;
        }
        float cloudHeight = level.effects().getCloudHeight();
        if (Float.isNaN(cloudHeight)) {
            return true;
        }
        // 1.21.1 draws the layer at cloud height + 0.33 (native cloud_height 192.33).
        float bottomY = cloudHeight + 0.33F;
        CloudStatus status = minecraft.options.getCloudsType();
        int radiusBlocks = range * 16;
        int radiusCells = Mth.ceil(radiusBlocks / CELL_SIZE);

        float relativeBottomY = (float) (bottomY - camY);
        float relativeTopY = relativeBottomY + CELL_HEIGHT;
        RelativeCameraPos relativePos = relativeTopY < 0.0F ? RelativeCameraPos.ABOVE_CLOUDS
                : relativeBottomY > 0.0F ? RelativeCameraPos.BELOW_CLOUDS : RelativeCameraPos.INSIDE_CLOUDS;

        float cloudOffset = (float) (level.getGameTime() % (this.texture.width() * 400L)) + partialTick;
        double cloudX = camX + cloudOffset * 0.030000001F;
        double cloudZ = camZ + 3.96F;
        double widthBlocks = this.texture.width() * 12.0;
        double heightBlocks = this.texture.height() * 12.0;
        cloudX -= Mth.floor(cloudX / widthBlocks) * widthBlocks;
        cloudZ -= Mth.floor(cloudZ / heightBlocks) * heightBlocks;
        int cellX = Mth.floor(cloudX / 12.0);
        int cellZ = Mth.floor(cloudZ / 12.0);
        float xInCell = (float) (cloudX - cellX * 12.0F);
        float zInCell = (float) (cloudZ - cellZ * 12.0F);
        boolean fancy = status == CloudStatus.FANCY;

        Vec3 cloudColor = level.getCloudColor(partialTick);

        if (this.needsRebuild || cellX != this.prevCellX || cellZ != this.prevCellZ || relativePos != this.prevRelativePos
                || status != this.prevStatus || radiusCells != this.prevRadius) {
            this.needsRebuild = false;
            this.prevCellX = cellX;
            this.prevCellZ = cellZ;
            this.prevRelativePos = relativePos;
            this.prevStatus = status;
            this.prevRadius = radiusCells;
            this.rebuild(relativePos, cellX, cellZ, fancy, radiusCells);
        }
        if (this.empty || this.buffer == null) {
            return true;
        }

        ShaderInstance shader = this.shader;
        if (shader.getUniform("CloudOffset") != null) {
            shader.getUniform("CloudOffset").set(-xInCell, relativeBottomY, -zInCell);
        }
        if (shader.getUniform("CloudColor") != null) {
            shader.getUniform("CloudColor").set((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 0.8F);
        }
        if (shader.getUniform("CloudEnd") != null) {
            shader.getUniform("CloudEnd").set(cloudFogEnd(minecraft, range));
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);
        RenderSystem.depthMask(true);
        if (fancy) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        this.buffer.bind();
        this.buffer.drawWithShader(modelView, projection, shader);
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        return true;
    }

    /**
     * Native AtmosphericFogEnvironment: {@code cloudEnd = min(cloudRange * 16, cloud_fog_end_distance 2048)}; under boss
     * world fog, in a fluid or with blindness/darkness the cloud end follows the environment fog end (here the 1.21.1
     * terrain fog end set just before the clouds are drawn).
     */
    private static float cloudFogEnd(Minecraft minecraft, int range) {
        float end = Math.min(range * 16.0F, 2048.0F);
        Entity camera = minecraft.gameRenderer.getMainCamera().getEntity();
        boolean special = minecraft.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE
                || minecraft.gui.getBossOverlay().shouldCreateWorldFog()
                || camera instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
        if (special) {
            end = Math.min(end, RenderSystem.getShaderFogEnd());
        }
        return end;
    }

    private void rebuild(RelativeCameraPos relativePos, int centerX, int centerZ, boolean extrude, int radiusCells) {
        TextureData texture = this.texture;
        int faces = 0;
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(1 << 20)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            // Native buildMesh: rings of growing Manhattan distance, inside the circle of radiusCells.
            for (int ring = 0; ring <= 2 * radiusCells; ring++) {
                for (int relX = -ring; relX <= ring; relX++) {
                    int relZ = ring - Math.abs(relX);
                    if (relZ >= 0 && relZ <= radiusCells && relX * relX + relZ * relZ <= radiusCells * radiusCells) {
                        if (relZ != 0) {
                            faces += this.tryBuildCell(builder, texture, relativePos, centerX, centerZ, extrude, relX, -relZ, r, g, b);
                        }
                        faces += this.tryBuildCell(builder, texture, relativePos, centerX, centerZ, extrude, relX, relZ, r, g, b);
                    }
                }
            }
            MeshData mesh = builder.build();
            if (this.buffer == null) {
                this.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            }
            this.empty = mesh == null || faces == 0;
            if (mesh != null) {
                this.buffer.bind();
                this.buffer.upload(mesh);
                VertexBuffer.unbind();
            }
        }
    }

    private int tryBuildCell(BufferBuilder builder, TextureData texture, RelativeCameraPos relativePos, int cellX, int cellZ,
                             boolean extrude, int relX, int relZ, float r, float g, float b) {
        int indexX = Math.floorMod(cellX + relX, texture.width());
        int indexY = Math.floorMod(cellZ + relZ, texture.height());
        long cell = texture.cells()[indexX + indexY * texture.width()];
        if (cell == 0L) {
            return 0;
        }
        if (!extrude) {
            // Native buildFlatCell: one bottom face with the top colour.
            face(builder, relX, relZ, Direction.DOWN, false, true, r, g, b);
            return 1;
        }
        int faces = 0;
        if (relativePos != RelativeCameraPos.BELOW_CLOUDS) {
            face(builder, relX, relZ, Direction.UP, false, false, r, g, b);
            faces++;
        }
        if (relativePos != RelativeCameraPos.ABOVE_CLOUDS) {
            face(builder, relX, relZ, Direction.DOWN, false, false, r, g, b);
            faces++;
        }
        if ((cell >> 3 & 1L) != 0L && relZ > 0) {
            face(builder, relX, relZ, Direction.NORTH, false, false, r, g, b);
            faces++;
        }
        if ((cell >> 1 & 1L) != 0L && relZ < 0) {
            face(builder, relX, relZ, Direction.SOUTH, false, false, r, g, b);
            faces++;
        }
        if ((cell & 1L) != 0L && relX > 0) {
            face(builder, relX, relZ, Direction.WEST, false, false, r, g, b);
            faces++;
        }
        if ((cell >> 2 & 1L) != 0L && relX < 0) {
            face(builder, relX, relZ, Direction.EAST, false, false, r, g, b);
            faces++;
        }
        if (Math.abs(relX) <= 1 && Math.abs(relZ) <= 1) {
            for (Direction direction : Direction.values()) {
                face(builder, relX, relZ, direction, true, false, r, g, b);
                faces++;
            }
        }
        return faces;
    }

    private static void face(BufferBuilder builder, int cellX, int cellZ, Direction direction, boolean inside, boolean topColor,
                             float r, float g, float b) {
        int dir = direction.get3DDataValue();
        float shade = topColor ? SHADES[1] : SHADES[dir];
        float[][] vertices = VERTICES[dir];
        for (int i = 0; i < 4; i++) {
            float[] v = vertices[inside ? 3 - i : i];
            builder.addVertex((v[0] + cellX) * CELL_SIZE, v[1] * CELL_HEIGHT, (v[2] + cellZ) * CELL_SIZE)
                    .setColor(shade * r, shade * g, shade * b, 1.0F);
        }
    }
}
