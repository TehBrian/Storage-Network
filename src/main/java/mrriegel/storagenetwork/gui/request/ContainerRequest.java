package mrriegel.storagenetwork.gui.request;

import java.util.ArrayList;
import java.util.List;

import mrriegel.storagenetwork.handler.GuiHandler;
import mrriegel.storagenetwork.network.PacketHandler;
import mrriegel.storagenetwork.network.StacksMessage;
import mrriegel.storagenetwork.network.SyncMessage;
import mrriegel.storagenetwork.tile.TileMaster;
import mrriegel.storagenetwork.tile.TileRequest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

public class ContainerRequest extends Container {
	public InventoryPlayer playerInv;
	public TileRequest tile;
	public InventoryCraftResult result;
	public InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
	public InventoryRequest back;
	String inv = "";
	SlotCrafting x;
	long lastTime;

	public ContainerRequest(final TileRequest tile, final InventoryPlayer playerInv) {
		this.tile = tile;
		this.playerInv = playerInv;
		lastTime = System.currentTimeMillis();
		back = new InventoryRequest(tile);
		result = new InventoryCraftResult();
		NBTTagCompound nbt = new NBTTagCompound();
		tile.writeToNBT(nbt);
		NBTTagList invList = nbt.getTagList("matrix", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < invList.tagCount(); i++) {
			NBTTagCompound stackTag = invList.getCompoundTagAt(i);
			int slot = stackTag.getByte("Slot");
			craftMatrix.setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(stackTag));
		}

		if (!tile.getWorld().isRemote) {
			TileMaster t = (TileMaster) tile.getWorld().getTileEntity(tile.getMaster());
			PacketHandler.INSTANCE.sendTo(new StacksMessage(t.getStacks(), t.getCraftableStacks(), GuiHandler.REQUEST), (EntityPlayerMP) playerInv.player);
		}
		x = new SlotCrafting(playerInv.player, craftMatrix, result, 0, 101, 128) {
			@Override
			public void onPickupFromSlot(EntityPlayer playerIn, ItemStack stack) {
				if (playerIn.worldObj.isRemote) {
					return;
				}
				List<ItemStack> lis = new ArrayList<ItemStack>();
				for (int i = 0; i < craftMatrix.getSizeInventory(); i++)
					lis.add(craftMatrix.getStackInSlot(i));
				super.onPickupFromSlot(playerIn, stack);
				TileMaster t = (TileMaster) tile.getWorld().getTileEntity(tile.getMaster());
				detectAndSendChanges();
				for (int i = 0; i < craftMatrix.getSizeInventory(); i++)
					if (craftMatrix.getStackInSlot(i) == null) {
						ItemStack req = t.request(lis.get(i), 1, true, true, false, false);
						craftMatrix.setInventorySlotContents(i, req);
					}
				PacketHandler.INSTANCE.sendTo(new StacksMessage(t.getStacks(), t.getCraftableStacks(), GuiHandler.REQUEST), (EntityPlayerMP) playerIn);
				detectAndSendChanges();
			}
		};
		this.addSlotToContainer(x);
		int index = 0;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				this.addSlotToContainer(new Slot(craftMatrix, index++, 8 + j * 18, 110 + i * 18));
			}
		}
		index = 0;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 2; ++j) {
				this.addSlotToContainer(new Slot(back, index++, 134 + j * 18, 110 + i * 18));
			}
		}

		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlotToContainer(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 174 + i * 18));
			}
		}
		for (int i = 0; i < 9; ++i) {
			this.addSlotToContainer(new Slot(playerInv, i, 8 + i * 18, 232));
		}
		this.onCraftMatrixChanged(this.craftMatrix);

	}

	String get() {
		String s = "";
		for (int i = 0; i < back.INVSIZE; i++)
			if (back.getStackInSlot(i) != null)
				s += back.getStackInSlot(i).toString();
		for (int i = 0; i < craftMatrix.getSizeInventory(); i++)
			if (craftMatrix.getStackInSlot(i) != null)
				s += craftMatrix.getStackInSlot(i).toString();
		return s;
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventoryIn) {
		this.result.setInventorySlotContents(0, CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, tile.getWorld()));
	}

	@Override
	public void onContainerClosed(EntityPlayer playerIn) {
		slotChanged();
		super.onContainerClosed(playerIn);
	}

	@Override
	public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer playerIn) {
		lastTime = System.currentTimeMillis();
		return super.slotClick(slotId, clickedButton, mode, playerIn);
	}

	public void slotChanged() {
		NBTTagCompound nbt = new NBTTagCompound();
		tile.writeToNBT(nbt);
		NBTTagList invList = new NBTTagList();
		for (int i = 0; i < 6; i++) {
			if (back.getStackInSlot(i) != null) {
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("Slot", (byte) i);
				back.getStackInSlot(i).writeToNBT(stackTag);
				invList.appendTag(stackTag);
			}
		}
		nbt.setTag("back", invList);
		invList = new NBTTagList();
		for (int i = 0; i < 9; i++) {
			if (craftMatrix.getStackInSlot(i) != null) {
				NBTTagCompound stackTag = new NBTTagCompound();
				stackTag.setByte("Slot", (byte) i);
				craftMatrix.getStackInSlot(i).writeToNBT(stackTag);
				invList.appendTag(stackTag);
			}
		}
		nbt.setTag("matrix", invList);
		tile.readFromNBT(nbt);
		// detectAndSendChanges();
		if (!tile.getWorld().isRemote)
			PacketHandler.INSTANCE.sendTo(new SyncMessage(back.getStackInSlot(0), back.getStackInSlot(1), back.getStackInSlot(2), back.getStackInSlot(3), back.getStackInSlot(4), back.getStackInSlot(5)), (EntityPlayerMP) playerInv.player);

	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int slotIndex) {
		ItemStack itemstack = null;
		Slot slot = this.inventorySlots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			if (slot.getSlotIndex() == x.getSlotIndex())
				if (x.crafted + itemstack.stackSize > itemstack.getMaxStackSize()) {
					x.crafted = 0;
					return null;
				}
			if (slotIndex <= 15) {
				if (!this.mergeItemStack(itemstack1, 15, 15 + 37, true)) {
					x.crafted = 0;
					return null;
				}
				slot.onSlotChange(itemstack1, itemstack);
			} else {
				if (!this.mergeItemStack(itemstack1, 10, 16, false)) {
					x.crafted = 0;
					return null;
				}
			}
			if (itemstack1.stackSize == 0) {
				slot.putStack((ItemStack) null);
			} else {
				slot.onSlotChanged();
			}

			if (itemstack1.stackSize == itemstack.stackSize) {
				x.crafted = 0;
				return null;
			}
			slot.onPickupFromSlot(playerIn, itemstack1);
			if (slot.getSlotIndex() == x.getSlotIndex()) {
				x.crafted += itemstack.stackSize;
			}
		} else
			x.crafted = 0;

		return itemstack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		if (tile == null || tile.getMaster() == null || !(tile.getWorld().getTileEntity(tile.getMaster()) instanceof TileMaster))
			return false;
		TileMaster t = (TileMaster) tile.getWorld().getTileEntity(tile.getMaster());
		if (!tile.getWorld().isRemote && tile.getWorld().getTotalWorldTime() % 50 == 0) {
			PacketHandler.INSTANCE.sendTo(new StacksMessage(t.getStacks(), t.getCraftableStacks(), GuiHandler.REQUEST), (EntityPlayerMP) playerInv.player);
		}
		if (!inv.equals(get())) {
			slotChanged();
			inv = get();
		}

		if (x.crafted != 0 && Math.abs(System.currentTimeMillis() - lastTime) > 500) {
			x.crafted = 0;
		}
		return true;
	}

	@Override
	public boolean canMergeSlot(ItemStack stack, Slot p_94530_2_) {
		return p_94530_2_.inventory != this.result && super.canMergeSlot(stack, p_94530_2_);
	}

}