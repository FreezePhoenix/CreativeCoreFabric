package team.creative.creativecore.client.render.box;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import com.mojang.math.Vector3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.creativecore.client.render.face.FaceRenderType;
import team.creative.creativecore.client.render.face.IFaceRenderType;
import team.creative.creativecore.common.mod.OptifineHelper;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;
import team.creative.creativecore.common.util.math.geo.NormalPlane;
import team.creative.creativecore.common.util.math.geo.Ray2d;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.creativecore.mixin.VertexFormatAccessor;

@Environment(EnvType.CLIENT)
public class RenderBox extends AlignedBox {
    private static final VectorFan DOWN = new VectorFanSimple(new Vec3f[] { new Vec3f(0, 0, 1), new Vec3f(0, 0, 0), new Vec3f(1, 0, 0), new Vec3f(1, 0, 1) });
    private static final VectorFan UP = new VectorFanSimple(new Vec3f[] { new Vec3f(0, 1, 0), new Vec3f(0, 1, 1), new Vec3f(1, 1, 1), new Vec3f(1, 1, 0) });
    private static final VectorFan NORTH = new VectorFanSimple(new Vec3f[] { new Vec3f(1, 1, 0), new Vec3f(1, 0, 0), new Vec3f(0, 0, 0), new Vec3f(0, 1, 0) });
    private static final VectorFan SOUTH = new VectorFanSimple(new Vec3f[] { new Vec3f(0, 1, 1), new Vec3f(0, 0, 1), new Vec3f(1, 0, 1), new Vec3f(1, 1, 1) });
    private static final VectorFan WEST = new VectorFanSimple(new Vec3f[] { new Vec3f(0, 1, 0), new Vec3f(0, 0, 0), new Vec3f(0, 0, 1), new Vec3f(0, 1, 1) });
    private static final VectorFan EAST = new VectorFanSimple(new Vec3f[] { new Vec3f(1, 1, 1), new Vec3f(1, 0, 1), new Vec3f(1, 0, 0), new Vec3f(1, 1, 0) });
    
    public BlockState state;
    public int color = -1;
    
    public boolean keepVU = false;
    public boolean allowOverlap = false;
    public boolean doesNeedQuadUpdate = true;
    public boolean needsResorting = false;
    public boolean emissive = false;
    public Object customData;
    private IFaceRenderType renderEast = FaceRenderType.INSIDE_RENDERED;
    private IFaceRenderType renderWest = FaceRenderType.INSIDE_RENDERED;
    private IFaceRenderType renderUp = FaceRenderType.INSIDE_RENDERED;
    private IFaceRenderType renderDown = FaceRenderType.INSIDE_RENDERED;
    private IFaceRenderType renderSouth = FaceRenderType.INSIDE_RENDERED;
    private IFaceRenderType renderNorth = FaceRenderType.INSIDE_RENDERED;
    private Object quadEast = null;
    private Object quadWest = null;
    private Object quadUp = null;
    private Object quadDown = null;
    private Object quadSouth = null;
    private Object quadNorth = null;
    
    public RenderBox(AlignedBox cube) {
        super(cube);
    }
    
    public RenderBox(AlignedBox cube, RenderBox box) {
        super(cube);
        this.state = box.state;
        this.color = box.color;
        this.renderEast = box.renderEast;
        this.renderWest = box.renderWest;
        this.renderUp = box.renderUp;
        this.renderDown = box.renderDown;
        this.renderSouth = box.renderSouth;
        this.renderNorth = box.renderNorth;
    }
    
    public RenderBox(AlignedBox cube, BlockState state) {
        super(cube);
        this.state = state;
    }
    
    public RenderBox(AlignedBox cube, Block block) {
        this(cube, block.defaultBlockState());
    }
    
    public RenderBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, BlockState state) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
        this.state = state;
    }
    
    public RenderBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Block block) {
        this(minX, minY, minZ, maxX, maxY, maxZ, block.defaultBlockState());
    }
    
    private static int uvOffset(VertexFormat format) {
        for (int i = 0; i < format.getElements().size(); i++) {
            if (format.getElements().get(i).getUsage() == Usage.UV) {
                return ((VertexFormatAccessor) format).getOffsets().getInt(i);
            }
        }
        return -1;
    }
    
    public RenderBox setColor(int color) {
        this.color = color;
        return this;
    }
    
    public RenderBox setKeepUV(boolean keep) {
        this.keepVU = keep;
        return this;
    }
    
    public void setQuad(Facing facing, List<BakedQuad> quads) {
        Object quad = quads == null || quads.isEmpty() ? null : quads.size() == 1 ? quads.get(0) : quads;
        switch (facing) {
        case DOWN -> quadDown = quad;
        case EAST -> quadEast = quad;
        case NORTH -> quadNorth = quad;
        case SOUTH -> quadSouth = quad;
        case UP -> quadUp = quad;
        case WEST -> quadWest = quad;
        }
    }
    
    public Object getQuad(Facing facing) {
        return switch (facing) {
        case DOWN -> quadDown;
        case EAST -> quadEast;
        case NORTH -> quadNorth;
        case SOUTH -> quadSouth;
        case UP -> quadUp;
        case WEST -> quadWest;
        };
    }
    
    public int countQuads() {
        int quads = 0;
        if (quadUp != null)
            quads += quadUp instanceof List ? ((List) quadUp).size() : 1;
        if (quadDown != null)
            quads += quadDown instanceof List ? ((List) quadDown).size() : 1;
        if (quadEast != null)
            quads += quadEast instanceof List ? ((List) quadEast).size() : 1;
        if (quadWest != null)
            quads += quadWest instanceof List ? ((List) quadWest).size() : 1;
        if (quadSouth != null)
            quads += quadSouth instanceof List ? ((List) quadSouth).size() : 1;
        if (quadNorth != null)
            quads += quadNorth instanceof List ? ((List) quadNorth).size() : 1;
        return quads;
    }
    
    public void setType(Facing facing, IFaceRenderType renderer) {
        switch (facing) {
        case DOWN -> renderDown = renderer;
        case EAST -> renderEast = renderer;
        case NORTH -> renderNorth = renderer;
        case SOUTH -> renderSouth = renderer;
        case UP -> renderUp = renderer;
        case WEST -> renderWest = renderer;
        }
    }
    
    public IFaceRenderType getType(Facing facing) {
        return switch (facing) {
        case DOWN -> renderDown;
        case EAST -> renderEast;
        case NORTH -> renderNorth;
        case SOUTH -> renderSouth;
        case UP -> renderUp;
        case WEST -> renderWest;
        };
    }
    
    public boolean renderSide(Facing facing) {
        return switch (facing) {
        case DOWN -> renderDown.shouldRender();
        case EAST -> renderEast.shouldRender();
        case NORTH -> renderNorth.shouldRender();
        case SOUTH -> renderSouth.shouldRender();
        case UP -> renderUp.shouldRender();
        case WEST -> renderWest.shouldRender();
        };
    }
    
    public boolean intersectsWithFace(Facing facing, RenderInformationHolder holder, BlockPos offset) {
        return switch (facing.axis) {
        case X -> holder.maxY > this.minY - offset.getY() && holder.minY < this.maxY - offset.getY() && holder.maxZ > this.minZ - offset.getZ() && holder.minZ < this.maxZ - offset
                .getZ();
        case Y -> holder.maxX > this.minX - offset.getX() && holder.minX < this.maxX - offset.getX() && holder.maxZ > this.minZ - offset.getZ() && holder.minZ < this.maxZ - offset
                .getZ();
        case Z -> holder.maxX > this.minX - offset.getX() && holder.minX < this.maxX - offset.getX() && holder.maxY > this.minY - offset.getY() && holder.minY < this.maxY - offset
                .getY();
        };
    }
    
    protected Object getRenderQuads(Facing facing) {
        if (getType(facing).hasCachedFans())
            return getType(facing).getCachedFans();
        return switch (facing) {
        case DOWN -> DOWN;
        case EAST -> EAST;
        case NORTH -> NORTH;
        case SOUTH -> SOUTH;
        case UP -> UP;
        case WEST -> WEST;
        };
    }
    
    protected float getOffsetX() {
        return minX;
    }
    
    protected float getOffsetY() {
        return minY;
    }
    
    protected float getOffsetZ() {
        return minZ;
    }
    
    protected float getOverallScale(Facing facing) {
        return getType(facing).getScale();
    }
    
    protected float getScaleX() {
        return maxX - minX;
    }
    
    protected float getScaleY() {
        return maxY - minY;
    }
    
    protected float getScaleZ() {
        return maxZ - minZ;
    }
    
    protected boolean scaleAndOffsetQuads(Facing facing) {
        return true;
    }
    
    protected boolean onlyScaleOnceNoOffset(Facing facing) {
        return getType(facing).hasCachedFans();
    }
    
    public void deleteQuadCache() {
        doesNeedQuadUpdate = true;
        quadEast = null;
        quadWest = null;
        quadUp = null;
        quadDown = null;
        quadSouth = null;
        quadNorth = null;
    }
    
    protected boolean previewScalingAndOffset() {
        return true;
    }
    
    public float getPreviewOffX() {
        return minX;
    }
    
    public float getPreviewOffY() {
        return minY;
    }
    
    public float getPreviewOffZ() {
        return minZ;
    }
    
    public float getPreviewScaleX() {
        return maxX - minX;
    }
    
    public float getPreviewScaleY() {
        return maxY - minY;
    }
    
    public float getPreviewScaleZ() {
        return maxZ - minZ;
    }
    
    public void renderPreview(PoseStack pose, BufferBuilder builder, int alpha) {
        int red = ColorUtils.red(color);
        int green = ColorUtils.green(color);
        int blue = ColorUtils.blue(color);
        
        if (previewScalingAndOffset()) {
            for (int i = 0; i < Facing.values().length; i++) {
                Object renderQuads = getRenderQuads(Facing.values()[i]);
                if (renderQuads instanceof List)
                    for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                        ((List<VectorFan>) renderQuads).get(j).renderPreview(pose.last()
                                .pose(), builder, getPreviewOffX(), getPreviewOffY(), getPreviewOffZ(), getPreviewScaleX(), getPreviewScaleY(), getPreviewScaleZ(), red, green, blue, alpha);
                else if (renderQuads instanceof VectorFan)
                    ((VectorFan) renderQuads).renderPreview(pose.last()
                            .pose(), builder, getPreviewOffX(), getPreviewOffY(), getPreviewOffZ(), getPreviewScaleX(), getPreviewScaleY(), getPreviewScaleZ(), red, green, blue, alpha);
            }
        } else {
            for (int i = 0; i < Facing.values().length; i++) {
                Object renderQuads = getRenderQuads(Facing.values()[i]);
                if (renderQuads instanceof List)
                    for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                        ((List<VectorFan>) renderQuads).get(j).renderPreview(pose.last().pose(), builder, red, green, blue, alpha);
                else if (renderQuads instanceof VectorFan)
                    ((VectorFan) renderQuads).renderPreview(pose.last().pose(), builder, red, green, blue, alpha);
            }
        }
    }
    
    public void renderLines(PoseStack pose, BufferBuilder builder, int alpha) {
        int red = ColorUtils.red(color);
        int green = ColorUtils.green(color);
        int blue = ColorUtils.blue(color);
        
        if (red == 1 && green == 1 && blue == 1)
            red = green = blue = 0;
        
        if (previewScalingAndOffset()) {
            for (int i = 0; i < Facing.values().length; i++) {
                Object renderQuads = getRenderQuads(Facing.values()[i]);
                if (renderQuads instanceof List)
                    for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                        ((List<VectorFan>) renderQuads).get(j).renderLines(pose.last()
                                .pose(), builder, getPreviewOffX(), getPreviewOffY(), getPreviewOffZ(), getPreviewScaleX(), getPreviewScaleY(), getPreviewScaleZ(), red, green, blue, alpha);
                else if (renderQuads instanceof VectorFan)
                    ((VectorFan) renderQuads).renderLines(pose.last()
                            .pose(), builder, getPreviewOffX(), getPreviewOffY(), getPreviewOffZ(), getPreviewScaleX(), getPreviewScaleY(), getPreviewScaleZ(), red, green, blue, alpha);
            }
        } else {
            for (int i = 0; i < Facing.values().length; i++) {
                Object renderQuads = getRenderQuads(Facing.values()[i]);
                if (renderQuads instanceof List)
                    for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                        ((List<VectorFan>) renderQuads).get(j).renderLines(pose.last().pose(), builder, red, green, blue, alpha);
                else if (renderQuads instanceof VectorFan)
                    ((VectorFan) renderQuads).renderLines(pose.last().pose(), builder, red, green, blue, alpha);
            }
        }
    }
    
    public void renderLines(PoseStack pose, BufferBuilder builder, int alpha, Vector3d center, double grow) {
        int red = ColorUtils.red(color);
        int green = ColorUtils.green(color);
        int blue = ColorUtils.blue(color);
        
        if (red == 1 && green == 1 && blue == 1)
            red = green = blue = 0;
        
        if (previewScalingAndOffset()) {
            for (int i = 0; i < Facing.values().length; i++) {
                Object renderQuads = getRenderQuads(Facing.values()[i]);
                if (renderQuads instanceof List)
                    for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                        ((List<VectorFan>) renderQuads).get(j).renderLines(pose.last()
                                .pose(), builder, getPreviewOffX(), getPreviewOffY(), getPreviewOffZ(), getPreviewScaleX(), getPreviewScaleY(), getPreviewScaleZ(), red, green, blue, alpha, center, grow);
                else if (renderQuads instanceof VectorFan)
                    ((VectorFan) renderQuads).renderLines(pose.last()
                            .pose(), builder, getPreviewOffX(), getPreviewOffY(), getPreviewOffZ(), getPreviewScaleX(), getPreviewScaleY(), getPreviewScaleZ(), red, green, blue, alpha, center, grow);
            }
        } else {
            for (int i = 0; i < Facing.values().length; i++) {
                Object renderQuads = getRenderQuads(Facing.values()[i]);
                if (renderQuads instanceof List)
                    for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                        ((List<VectorFan>) renderQuads).get(j).renderLines(pose.last().pose(), builder, red, green, blue, alpha, center, grow);
                else if (renderQuads instanceof VectorFan)
                    ((VectorFan) renderQuads).renderLines(pose.last().pose(), builder, red, green, blue, alpha, center, grow);
            }
        }
    }
    
    public boolean isTranslucent() {
        if (ColorUtils.isTransparent(color))
            return true;
        return !state.getMaterial().isSolidBlocking() || !state.getMaterial().isSolid();
    }
    
    protected List<BakedQuad> getBakedQuad(LevelAccessor level, BakedModel blockModel, BlockState state, Facing facing, BlockPos pos, Random rand) {
        return OptifineHelper.getBakedQuad(blockModel.getQuads(state, facing.toVanilla(), rand), level, state, facing, pos, rand);
    }
    
    public List<BakedQuad> getBakedQuad(LevelAccessor level, @Nullable BlockPos pos, BlockPos offset, BlockState state, BakedModel blockModel, Facing facing, Random rand, boolean overrideTint, int defaultColor) {
        List<BakedQuad> blockQuads = getBakedQuad(level, blockModel, state, facing, pos, rand);
        
        if (blockQuads.isEmpty())
            return Collections.emptyList();
        RenderInformationHolder holder = new RenderInformationHolder(DefaultVertexFormat.BLOCK, facing, this.color != -1 ? this.color : defaultColor);
        holder.offset = offset;
        
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedQuad blockQuad : blockQuads) {
            
            holder.setQuad(blockQuad, overrideTint, defaultColor);
            if (!needsResorting && OptifineHelper.isEmissive(holder.quad.getSprite()))
                needsResorting = true;
            
            int[] data = holder.quad.getVertices();
            
            int index = 0;
            int uvIndex = index + holder.uvOffset / 4;
            float tempMinX = Float.intBitsToFloat(data[index]);
            float tempMinY = Float.intBitsToFloat(data[index + 1]);
            float tempMinZ = Float.intBitsToFloat(data[index + 2]);
            
            float tempU = Float.intBitsToFloat(data[uvIndex]);
            
            holder.uvInverted = false;
            
            index = 1 * holder.format.getIntegerSize();
            uvIndex = index + holder.uvOffset / 4;
            if (tempMinX != Float.intBitsToFloat(data[index])) {
                if (tempU != Float.intBitsToFloat(data[uvIndex]))
                    holder.uvInverted = Axis.X != facing.getUAxis();
                else
                    holder.uvInverted = Axis.X != facing.getVAxis();
            } else if (tempMinY != Float.intBitsToFloat(data[index + 1])) {
                if (tempU != Float.intBitsToFloat(data[uvIndex]))
                    holder.uvInverted = Axis.Y != facing.getUAxis();
                else
                    holder.uvInverted = Axis.Y != facing.getVAxis();
            } else {
                if (tempU != Float.intBitsToFloat(data[uvIndex]))
                    holder.uvInverted = Axis.Z != facing.getUAxis();
                else
                    holder.uvInverted = Axis.Z != facing.getVAxis();
            }
            
            index = 2 * holder.format.getIntegerSize();
            float tempMaxX = Float.intBitsToFloat(data[index]);
            float tempMaxY = Float.intBitsToFloat(data[index + 1]);
            float tempMaxZ = Float.intBitsToFloat(data[index + 2]);
            
            holder.setBounds(tempMinX, tempMinY, tempMinZ, tempMaxX, tempMaxY, tempMaxZ);
            
            // Check if it is intersecting, otherwise there is no need to render it
            if (!intersectsWithFace(facing, holder, offset))
                continue;
            
            uvIndex = holder.uvOffset / 4;
            float u1 = Float.intBitsToFloat(data[uvIndex]);
            float v1 = Float.intBitsToFloat(data[uvIndex + 1]);
            uvIndex = 2 * holder.format.getIntegerSize() + holder.uvOffset / 4;
            float u2 = Float.intBitsToFloat(data[uvIndex]);
            float v2 = Float.intBitsToFloat(data[uvIndex + 1]);
            
            if (holder.uvInverted) {
                holder.sizeU = facing.getV(tempMinX, tempMinY, tempMinZ) < facing.getV(tempMaxX, tempMaxY, tempMaxZ) ? u2 - u1 : u1 - u2;
                holder.sizeV = facing.getU(tempMinX, tempMinY, tempMinZ) < facing.getU(tempMaxX, tempMaxY, tempMaxZ) ? v2 - v1 : v1 - v2;
            } else {
                holder.sizeU = facing.getU(tempMinX, tempMinY, tempMinZ) < facing.getU(tempMaxX, tempMaxY, tempMaxZ) ? u2 - u1 : u1 - u2;
                holder.sizeV = facing.getV(tempMinX, tempMinY, tempMinZ) < facing.getV(tempMaxX, tempMaxY, tempMaxZ) ? v2 - v1 : v1 - v2;
            }
            
            Object renderQuads = getRenderQuads(holder.facing);
            if (renderQuads instanceof List)
                for (int j = 0; j < ((List<VectorFan>) renderQuads).size(); j++)
                    ((List<VectorFan>) renderQuads).get(j).generate(holder, quads);
            else if (renderQuads instanceof VectorFan)
                ((VectorFan) renderQuads).generate(holder, quads);
        }
        return quads;
        
    }
    
    private static class VectorFanSimple extends VectorFan {
        
        public VectorFanSimple(Vec3f[] coords) {
            super(coords);
            
        }
        
        @Override
        @Environment(EnvType.CLIENT)
        public void generate(RenderInformationHolder holder, List<BakedQuad> quads) {
            int index = 0;
            while (index < coords.length - 3) {
                generate(holder, coords[0], coords[index + 1], coords[index + 2], coords[index + 3], quads);
                index += 2;
            }
            if (index < coords.length - 2)
                generate(holder, coords[0], coords[index + 1], coords[index + 2], coords[index + 2], quads);
        }
        
        @Override
        protected boolean doMinMaxLate() {
            return true;
        }
        
    }
    
    public class RenderInformationHolder {
        
        public final Facing facing;
        public final int color;
        public final VertexFormat format;
        public final int uvOffset;
        public final boolean scaleAndOffset;
        public final float offsetX;
        public final float offsetY;
        public final float offsetZ;
        public final float scaleX;
        public final float scaleY;
        public final float scaleZ;
        public BlockPos offset;
        public boolean shouldOverrideColor;
        public BakedQuad quad;
        public NormalPlane normal;
        public Ray2d ray = new Ray2d(Axis.X, Axis.Y, 0, 0, 0, 0);
        public float minX;
        public float minY;
        public float minZ;
        public float maxX;
        public float maxY;
        public float maxZ;
        
        public float sizeX;
        public float sizeY;
        public float sizeZ;
        
        public boolean uvInverted;
        public float sizeU;
        public float sizeV;
        
        public RenderInformationHolder(VertexFormat format, Facing facing, int color) {
            this.color = color;
            this.format = format;
            this.facing = facing;
            this.uvOffset = uvOffset(format);
            
            RenderBox box = getBox();
            scaleAndOffset = box.scaleAndOffsetQuads(facing);
            if (scaleAndOffset) {
                if (box.onlyScaleOnceNoOffset(facing)) {
                    this.offsetX = this.offsetY = this.offsetZ = 0;
                    this.scaleX = this.scaleY = this.scaleZ = box.getOverallScale(facing);
                } else {
                    this.offsetX = box.getOffsetX();
                    this.offsetY = box.getOffsetY();
                    this.offsetZ = box.getOffsetZ();
                    this.scaleX = box.getScaleX();
                    this.scaleY = box.getScaleY();
                    this.scaleZ = box.getScaleZ();
                }
                
            } else {
                this.offsetX = this.offsetY = this.offsetZ = 0;
                this.scaleX = this.scaleY = this.scaleZ = 0;
            }
        }
        
        public void setQuad(BakedQuad quad, boolean overrideTint, int defaultColor) {
            this.quad = quad;
            this.shouldOverrideColor = overrideTint && (defaultColor == -1 || quad.isTinted()) && color != -1;
        }
        
        public void setBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
            
            this.sizeX = this.maxX - this.minX;
            this.sizeY = this.maxY - this.minY;
            this.sizeZ = this.maxZ - this.minZ;
        }
        
        public RenderBox getBox() {
            return RenderBox.this;
        }
        
        public boolean hasBounds() {
            return switch (facing.axis) {
            case X -> minY != 0 || maxY != 1 || minZ != 0 || maxZ != 1;
            case Y -> minX != 0 || maxX != 1 || minZ != 0 || maxZ != 1;
            case Z -> minX != 0 || maxX != 1 || minY != 0 || maxY != 1;
            };
        }
    }
    
}
