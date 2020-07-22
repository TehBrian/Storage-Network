package com.lothrazar.storagenetwork.block.cable.linkfilter;

import java.util.List;
import com.google.common.collect.Lists;
import com.lothrazar.storagenetwork.StorageNetwork;
import com.lothrazar.storagenetwork.api.IGuiPrivate;
import com.lothrazar.storagenetwork.capability.handler.FilterItemStackHandler;
import com.lothrazar.storagenetwork.gui.ButtonRequest;
import com.lothrazar.storagenetwork.gui.ButtonRequest.TextureEnum;
import com.lothrazar.storagenetwork.gui.ItemSlotNetwork;
import com.lothrazar.storagenetwork.network.CableDataMessage;
import com.lothrazar.storagenetwork.registry.PacketRegistry;
import com.lothrazar.storagenetwork.util.UtilTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class GuiCableFilter extends ContainerScreen<ContainerCableFilter> implements IGuiPrivate {

  private final ResourceLocation texture = new ResourceLocation(StorageNetwork.MODID, "textures/gui/cable.png");
  ContainerCableFilter containerCableLink;
  private ButtonRequest btnMinus;
  private ButtonRequest btnPlus;
  private ButtonRequest btnWhite;
  private ButtonRequest btnImport;
  private boolean isAllowlist;
  private List<ItemSlotNetwork> itemSlotsGhost;

  public GuiCableFilter(ContainerCableFilter containerCableFilter, PlayerInventory inv, ITextComponent name) {
    super(containerCableFilter, inv, name);
    this.containerCableLink = containerCableFilter;
  }

  @Override
  public void init() {
    super.init();
    this.isAllowlist = containerCableLink.cap.getFilter().isAllowList;
    int x = guiLeft + 7, y = guiTop + 8;
    btnMinus = addButton(new ButtonRequest(x, y, "", (p) -> {
      this.syncData(-1);
    }));
    btnMinus.setTextureId(TextureEnum.MINUS);
    x += 30;
    btnPlus = addButton(new ButtonRequest(x, y, "", (p) -> {
      this.syncData(+1);
    }));
    btnPlus.setTextureId(TextureEnum.PLUS);
    x += 20;
    btnWhite = addButton(new ButtonRequest(x, y, "", (p) -> {
      this.isAllowlist = !this.isAllowlist;
      this.syncData(0);
    }));
    x += 20;
    btnImport = addButton(new ButtonRequest(x, y, "", (p) -> {
      importFilterSlots();
    }));
    btnImport.setTextureId(TextureEnum.IMPORT);
  }

  private void importFilterSlots() {
    PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(CableDataMessage.CableMessageType.IMPORT_FILTER.ordinal()));
  }

  private void sendStackSlot(int value, ItemStack stack) {
    PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(CableDataMessage.CableMessageType.SAVE_FITLER.ordinal(), value, stack));
  }

  private void syncData(int priority) {
    PacketRegistry.INSTANCE.sendToServer(new CableDataMessage(CableDataMessage.CableMessageType.SYNC_DATA.ordinal(),
        priority, isAllowlist));
  }

  @Override
  public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
    renderBackground(ms);
    super.render(ms, mouseX, mouseY, partialTicks);
    //    renderHoveredToolTip(mouseX, mouseY);
    if (containerCableLink == null || containerCableLink.cap == null) {
      return;
    }
    btnWhite.setTextureId(this.isAllowlist ? TextureEnum.ALLOWLIST : TextureEnum.IGNORELIST);
  }

  @Override //drawGuiContainerForegroundLayer
  public void func_230451_b_(MatrixStack ms, int mouseX, int mouseY) {
    super.func_230451_b_(ms, mouseX, mouseY);
    int priority = containerCableLink.cap.getPriority();
    font.drawString(ms, String.valueOf(priority),
        30 - font.getStringWidth(String.valueOf(priority)) / 2,
        14,
        4210752);
    this.drawTooltips(ms, mouseX, mouseY);
  }

  private void drawTooltips(MatrixStack ms, final int mouseX, final int mouseY) {
    if (btnImport != null && btnImport.isMouseOver(mouseX, mouseY)) {
      renderTooltip(ms, Lists.newArrayList(new StringTextComponent("gui.storagenetwork.import")),
          mouseX - guiLeft, mouseY - guiTop, font);
    }
    if (btnWhite != null && btnWhite.isMouseOver(mouseX, mouseY)) {
      renderTooltip(ms, Lists.newArrayList(new StringTextComponent(this.isAllowlist
          ? "gui.storagenetwork.gui.whitelist"
          : "gui.storagenetwork.gui.blacklist")),
          mouseX - guiLeft, mouseY - guiTop, font);
    }
    if (btnMinus != null && btnMinus.isMouseOver(mouseX, mouseY)) {
      renderTooltip(ms, Lists.newArrayList(new StringTextComponent("gui.storagenetwork.priority.down")), mouseX - guiLeft, mouseY - guiTop, font);
    }
    if (btnPlus != null && btnPlus.isMouseOver(mouseX, mouseY)) {
      renderTooltip(ms, Lists.newArrayList(new StringTextComponent("gui.storagenetwork.priority.up")), mouseX - guiLeft, mouseY - guiTop, font);
    }
  }

  public static final int SLOT_SIZE = 18;

  @Override // drawGuiContainerBackgroundLayer
  protected void func_230450_a_(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    minecraft.getTextureManager().bindTexture(texture);
    int xCenter = (width - xSize) / 2;
    int yCenter = (height - ySize) / 2;
    blit(ms, xCenter, yCenter, 0, 0, xSize, ySize);
    itemSlotsGhost = Lists.newArrayList();
    //TODO: shared with GuiCableIO
    int rows = 2;
    int cols = 9;
    int index = 0;
    int y = 35;
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        //
        ItemStack stack = containerCableLink.cap.getFilter().getStackInSlot(index);
        int x = 8 + col * SLOT_SIZE;
        itemSlotsGhost.add(new ItemSlotNetwork(this, stack, guiLeft + x, guiTop + y, stack.getCount(), guiLeft, guiTop, true));
        index++;
      }
      //move down to second row
      y += SLOT_SIZE;
    }
    for (ItemSlotNetwork s : itemSlotsGhost) {
      s.drawSlot(font, mouseX, mouseY);
    }
  }

  public void setFilterItems(List<ItemStack> stacks) {
    FilterItemStackHandler filter = this.containerCableLink.cap.getFilter();
    for (int i = 0; i < stacks.size(); i++) {
      ItemStack s = stacks.get(i);
      filter.setStackInSlot(i, s);
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
    ItemStack mouse = minecraft.player.inventory.getItemStack();
    for (int i = 0; i < this.itemSlotsGhost.size(); i++) {
      ItemSlotNetwork slot = itemSlotsGhost.get(i);
      if (slot.isMouseOverSlot((int) mouseX, (int) mouseY)) {
        if (slot.getStack().isEmpty() == false) {
          //i hit non-empty slot, clear it no matter what
          if (mouseButton == UtilTileEntity.MOUSE_BTN_RIGHT) {
            int direction = hasShiftDown() ? -1 : 1;
            int newCount = Math.min(64, slot.getStack().getCount() + direction);
            if (newCount < 1) {
              newCount = 1;
            }
            slot.getStack().setCount(newCount);
          }
          else {
            slot.setStack(ItemStack.EMPTY);
          }
          this.sendStackSlot(i, slot.getStack());
          return true;
        }
        else {
          //i hit an empty slot, save what im holding
          slot.setStack(mouse.copy());
          this.sendStackSlot(i, mouse.copy());
          return true;
        }
      }
    }
    return super.mouseClicked(mouseX, mouseY, mouseButton);
  }
  //  @Override
  //  public void renderStackToolTip(ItemStack stack, int x, int y) {
  //    super.renderTooltip(stack, x, y);
  //  }
  //
  //  @Override
  //  public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
  //    super.fillGradient(left, top, right, bottom, startColor, endColor);
  //  }

  @Override
  public boolean isInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
    return super.isPointInRegion(x, y, width, height, mouseX, mouseY);
  }
}