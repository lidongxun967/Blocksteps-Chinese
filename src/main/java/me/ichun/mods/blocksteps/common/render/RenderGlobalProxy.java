package me.ichun.mods.blocksteps.common.render;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import me.ichun.mods.blocksteps.common.Blocksteps;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ListChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VboChunkFactory;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector3f;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class RenderGlobalProxy extends RenderGlobal
{
    public boolean setupTerrain = false;

    public RenderGlobalProxy(Minecraft par1Minecraft)
    {
        super(par1Minecraft);
    }

    @Override
    public void deleteAllDisplayLists()
    {
        super.deleteAllDisplayLists();
        if (starGLCallList >= 0)
        {
            GLAllocation.deleteDisplayLists(starGLCallList);
        }
    }

    @Override
    public void loadRenderers()
    {
        if (this.theWorld != null && !setupTerrain)
        {
            this.displayListEntitiesDirty = true;
            Blocks.leaves.setGraphicsLevel(this.mc.gameSettings.fancyGraphics);
            Blocks.leaves2.setGraphicsLevel(this.mc.gameSettings.fancyGraphics);
            this.renderDistanceChunks = Blocksteps.config.renderDistance > 0 ? Blocksteps.config.renderDistance : this.mc.gameSettings.renderDistanceChunks;
            boolean flag = this.vboEnabled;
            this.vboEnabled = OpenGlHelper.useVbo();

            this.renderContainer = new RenderList();
            this.renderChunkFactory = new ListChunkFactoryBlocksteps();

            if (flag != this.vboEnabled)
            {
                this.generateStars();
                this.generateSky();
                this.generateSky2();
            }

            if (this.viewFrustum != null)
            {
                this.viewFrustum.deleteGlResources();
            }

            this.stopChunkUpdates();
            this.viewFrustum = new ViewFrustum(this.theWorld, this.mc.gameSettings.renderDistanceChunks, this, this.renderChunkFactory);

            if (this.theWorld != null)
            {
                Entity entity = this.mc.getRenderViewEntity();

                if (entity != null)
                {
                    this.viewFrustum.updateChunkPositions(entity.posX, entity.posZ);
                }
            }

            this.renderEntitiesStartupCounter = 2;
        }
    }

    @Override
    public void generateSky2()
    {
        if (this.sky2VBO != null)
        {
            this.sky2VBO.deleteGlBuffers();
        }

        if (this.glSkyList2 >= 0)
        {
            GLAllocation.deleteDisplayLists(this.glSkyList2);
            this.glSkyList2 = -1;
        }

        if (this.vboEnabled)
        {
            this.sky2VBO = new VertexBuffer(this.vertexBufferFormat);
        }
        else
        {
            this.glSkyList2 = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(this.glSkyList2, GL11.GL_COMPILE);
            GL11.glEndList();
        }
    }

    @Override
    public void generateSky()
    {
        if (this.skyVBO != null)
        {
            this.skyVBO.deleteGlBuffers();
        }

        if (this.glSkyList >= 0)
        {
            GLAllocation.deleteDisplayLists(this.glSkyList);
            this.glSkyList = -1;
        }

        if (this.vboEnabled)
        {
            this.skyVBO = new VertexBuffer(this.vertexBufferFormat);
        }
        else
        {
            this.glSkyList = GLAllocation.generateDisplayLists(1);
            GL11.glNewList(this.glSkyList, GL11.GL_COMPILE);
            GL11.glEndList();
        }
    }

    @Override
    public void renderEntities(Entity renderViewEntity, ICamera camera, float partialTicks)
    {
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        if (this.renderEntitiesStartupCounter > 0)
        {
            if (pass > 0) return;
            --this.renderEntitiesStartupCounter;
        }
        else
        {
            double d0 = renderViewEntity.prevPosX + (renderViewEntity.posX - renderViewEntity.prevPosX) * (double)partialTicks;
            double d1 = renderViewEntity.prevPosY + (renderViewEntity.posY - renderViewEntity.prevPosY) * (double)partialTicks;
            double d2 = renderViewEntity.prevPosZ + (renderViewEntity.posZ - renderViewEntity.prevPosZ) * (double)partialTicks;
            this.theWorld.theProfiler.startSection("prepare");
            TileEntityRendererDispatcher.instance.cacheActiveRenderInfo(this.theWorld, this.mc.getTextureManager(), this.mc.fontRendererObj, this.mc.getRenderViewEntity(), partialTicks);
            this.renderManager.cacheActiveRenderInfo(this.theWorld, this.mc.fontRendererObj, this.mc.getRenderViewEntity(), this.mc.pointedEntity, this.mc.gameSettings, partialTicks);
            if (pass == 0) // no indentation to shrink patch
            {
                this.countEntitiesTotal = 0;
                this.countEntitiesRendered = 0;
                this.countEntitiesHidden = 0;
            }
            Entity entity1 = this.mc.getRenderViewEntity();
            double d3 = entity1.lastTickPosX + (entity1.posX - entity1.lastTickPosX) * (double)partialTicks;
            double d4 = entity1.lastTickPosY + (entity1.posY - entity1.lastTickPosY) * (double)partialTicks;
            double d5 = entity1.lastTickPosZ + (entity1.posZ - entity1.lastTickPosZ) * (double)partialTicks;
            TileEntityRendererDispatcher.staticPlayerX = d3;
            TileEntityRendererDispatcher.staticPlayerY = d4;
            TileEntityRendererDispatcher.staticPlayerZ = d5;
            this.renderManager.setRenderPosition(d3, d4, d5);
            this.mc.entityRenderer.enableLightmap();
            this.theWorld.theProfiler.endStartSection("global");
            List list = this.theWorld.getLoadedEntityList();
            if (pass == 0) // no indentation to shrink patch
            {
                this.countEntitiesTotal = list.size();
            }
            int i;
            Entity entity2;

            for (i = 0; i < this.theWorld.weatherEffects.size(); ++i)
            {
                entity2 = (Entity)this.theWorld.weatherEffects.get(i);
                if (!entity2.shouldRenderInPass(pass)) continue;
                ++this.countEntitiesRendered;

                if (shouldRenderEntity(entity2) && entity2.isInRangeToRender3d(d0, d1, d2))
                {
                    this.renderManager.renderEntitySimple(entity2, partialTicks);
                }
            }

            this.theWorld.theProfiler.endStartSection("entities");
            Iterator iterator = this.renderInfos.iterator();
            RenderGlobal.ContainerLocalRenderInformation containerlocalrenderinformation;

            while (iterator.hasNext())
            {
                containerlocalrenderinformation = (RenderGlobal.ContainerLocalRenderInformation)iterator.next();
                Chunk chunk = this.theWorld.getChunkFromBlockCoords(containerlocalrenderinformation.renderChunk.getPosition());
                Iterator iterator2 = chunk.getEntityLists()[containerlocalrenderinformation.renderChunk.getPosition().getY() / 16].iterator();

                while (iterator2.hasNext())
                {
                    Entity entity3 = (Entity)iterator2.next();
                    if (!entity3.shouldRenderInPass(pass)) continue;
                    boolean flag2 = this.renderManager.shouldRender(entity3, camera, d0, d1, d2) || entity3.riddenByEntity == this.mc.thePlayer;

                    if (flag2)
                    {
                        boolean flag3 = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();

                        if (!shouldRenderEntity(entity3) || entity3 == this.mc.getRenderViewEntity() && this.mc.gameSettings.thirdPersonView == 0 && !flag3 || entity3.posY >= 0.0D && entity3.posY < 256.0D && !this.theWorld.isBlockLoaded(new BlockPos(entity3)))
                        {
                            continue;
                        }

                        ++this.countEntitiesRendered;
                        this.renderManager.renderEntitySimple(entity3, partialTicks);
                    }

                    if (!flag2 && entity3 instanceof EntityWitherSkull)
                    {
                        this.mc.getRenderManager().renderWitherSkull(entity3, partialTicks);
                    }
                }
            }

            this.theWorld.theProfiler.endStartSection("blockentities");
            RenderHelper.enableStandardItemLighting();
            iterator = this.renderInfos.iterator();
            TileEntity tileentity;

            while (iterator.hasNext())
            {
                containerlocalrenderinformation = (RenderGlobal.ContainerLocalRenderInformation)iterator.next();
                Iterator iterator1 = containerlocalrenderinformation.renderChunk.getCompiledChunk().getTileEntities().iterator();

                while (iterator1.hasNext())
                {
                    tileentity = (TileEntity)iterator1.next();
                    if (!tileentity.shouldRenderInPass(pass) || !camera.isBoundingBoxInFrustum(tileentity.getRenderBoundingBox())) continue;
                    TileEntityRendererDispatcher.instance.renderTileEntity(tileentity, partialTicks, -1);
                }
            }

            this.preRenderDamagedBlocks();
            iterator = this.damagedBlocks.values().iterator();

            while (iterator.hasNext())
            {
                DestroyBlockProgress destroyblockprogress = (DestroyBlockProgress)iterator.next();
                BlockPos blockpos = destroyblockprogress.getPosition();
                tileentity = this.theWorld.getTileEntity(blockpos);

                if (tileentity instanceof TileEntityChest)
                {
                    TileEntityChest tileentitychest = (TileEntityChest)tileentity;

                    if (tileentitychest.adjacentChestXNeg != null)
                    {
                        blockpos = blockpos.offset(EnumFacing.WEST);
                        tileentity = this.theWorld.getTileEntity(blockpos);
                    }
                    else if (tileentitychest.adjacentChestZNeg != null)
                    {
                        blockpos = blockpos.offset(EnumFacing.NORTH);
                        tileentity = this.theWorld.getTileEntity(blockpos);
                    }
                }

                Block block = this.theWorld.getBlockState(blockpos).getBlock();

                if (tileentity != null && tileentity.shouldRenderInPass(pass) && tileentity.canRenderBreaking() && camera.isBoundingBoxInFrustum(tileentity.getRenderBoundingBox()))
                {
                    TileEntityRendererDispatcher.instance.renderTileEntity(tileentity, partialTicks, destroyblockprogress.getPartialBlockDamage());
                }
            }

            this.postRenderDamagedBlocks();
            this.mc.entityRenderer.disableLightmap();
            this.mc.mcProfiler.endSection();
        }
    }

    public boolean shouldRenderEntity(Entity entity)
    {
        BlockPos pos = new BlockPos(entity);
        return Blocksteps.config.mapShowEntities == 1 && (Blocksteps.eventHandler.blocksToRender.contains(pos) || Blocksteps.eventHandler.blocksToRender.contains(pos.add(0, -1, 0)) || Blocksteps.eventHandler.blocksToRender.contains(pos.add(0, -2, 0)));
    }

    /**
     * Plays the specified record. Arg: recordName, x, y, z
     */
    @Override
    public void playRecord(String par1Str, BlockPos pos)
    {
    }

    /**
     * Plays the specified sound. Arg: soundName, x, y, z, volume, pitch
     */
    @Override
    public void playSound(String par1Str, double par2, double par4, double par6, float par8, float par9) {}

    /**
     * Plays sound to all near players except the player reference given
     */
    @Override
    public void playSoundToNearExcept(EntityPlayer par1EntityPlayer, String par2Str, double par3, double par5, double par7, float par9, float par10) {}

    @Override
    public void broadcastSound(int par1, BlockPos pos, int par5)
    {
    }

    /**
     * Plays a pre-canned sound effect along with potentially auxiliary data-driven one-shot behaviour (particles, etc).
     */
    @Override
    public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int p_180439_4_)
    {
    }
}
