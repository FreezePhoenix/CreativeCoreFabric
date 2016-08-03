package com.creativemd.creativecore.client.rendering.model;

import java.util.ArrayList;

import com.creativemd.creativecore.common.utils.CubeObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;

public interface ICreativeRendered {
	
	public ArrayList<CubeObject> getRenderingCubes(IBlockState state, TileEntity te, ItemStack stack);
	
	
	
}