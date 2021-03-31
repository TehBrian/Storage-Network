package com.lothrazar.storagenetwork.item.remote;

import com.lothrazar.storagenetwork.StorageNetwork;
import com.lothrazar.storagenetwork.api.DimPos;
import com.lothrazar.storagenetwork.block.main.TileMain;
import com.lothrazar.storagenetwork.gui.ContainerNetwork;
import com.lothrazar.storagenetwork.registry.SsnRegistry;
import com.lothrazar.storagenetwork.util.UtilInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class ContainerNetworkRemote extends ContainerNetwork {

  private final TileMain root;
  private ItemStack remote;

  public ContainerNetworkRemote(int id, PlayerInventory pInv) {
    super(SsnRegistry.REMOTE, id);
    this.remote = UtilInventory.getCurioRemote(pInv.player, SsnRegistry.INVENTORY_REMOTE).getRight();
    this.player = pInv.player;
    this.world = player.world;
    DimPos dp = DimPos.getPosStored(remote);
    if (dp == null) {
      StorageNetwork.LOGGER.error("Remote opening with null pos Stored {} ", remote);
    }
    this.root = dp.getTileEntity(TileMain.class, world);
    this.playerInv = pInv;
    bindPlayerInvo(this.playerInv);
    bindHotbar();
  }

  @Override
  public boolean canInteractWith(PlayerEntity playerIn) {
    //does not store itemstack inventory, and opens from curios so no security here. unless it dissapears
    return !remote.isEmpty();
  }

  @Override
  public TileMain getTileMain() {
    return root;
  }

  @Override
  public void slotChanged() {}

  @Override
  public boolean isCrafting() {
    return false;
  }
}
