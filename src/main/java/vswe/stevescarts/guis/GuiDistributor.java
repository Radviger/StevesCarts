package vswe.stevescarts.guis;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vswe.stevescarts.blocks.tileentities.TileEntityDistributor;
import vswe.stevescarts.blocks.tileentities.TileEntityManager;
import vswe.stevescarts.containers.ContainerDistributor;
import vswe.stevescarts.helpers.DistributorSetting;
import vswe.stevescarts.helpers.DistributorSide;
import vswe.stevescarts.helpers.Localization;
import vswe.stevescarts.helpers.ResourceHelper;

import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiDistributor extends GuiBase {
	private String mouseOverText;
	private static ResourceLocation texture;
	private int activeId;
	private TileEntityDistributor distributor;

	public GuiDistributor(final InventoryPlayer invPlayer, final TileEntityDistributor distributor) {
		super(new ContainerDistributor(invPlayer, distributor));
		activeId = -1;
		setXSize(255);
		setYSize(186);
		this.distributor = distributor;
	}

	@Override
	public void drawGuiForeground(final int x, final int y) {
		GlStateManager.disableLighting();
		getFontRenderer().drawString(Localization.GUI.DISTRIBUTOR.TITLE.translate(), 8, 6, 4210752);
		final TileEntityManager[] invs = distributor.getInventories();
		if (invs.length == 0) {
			getFontRenderer().drawString(Localization.GUI.DISTRIBUTOR.NOT_CONNECTED.translate(), 30, 40, 16728128);
		}
		if (mouseOverText != null && !mouseOverText.equals("")) {
			drawMouseOver(mouseOverText, x - getGuiLeft(), y - getGuiTop());
		}
		mouseOverText = null;
		GlStateManager.enableLighting();
	}

	private void drawMouseMover(final String str, final int x, final int y, final int[] rect) {
		if (inRect(x, y, rect)) {
			mouseOverText = str;
		}
	}

	@Override
	public void drawGuiBackground(final float f, int x, int y) {
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		final int j = getGuiLeft();
		final int k = getGuiTop();
		ResourceHelper.bindResource(GuiDistributor.texture);
		drawTexturedModalRect(j, k, 0, 0, xSize, ySize);
		x -= getGuiLeft();
		y -= getGuiTop();
		final TileEntityManager[] invs = distributor.getInventories();
		final List<DistributorSide> sides = distributor.getSides();
		int id = 0;
		for (final DistributorSide side : sides) {
			if (side.isEnabled(distributor)) {
				final int[] box = getSideBoxRect(id);
				int srcX = 0;
				if (inRect(x, y, box)) {
					srcX = box[2];
				}
				drawTexturedModalRect(j + box[0], k + box[1], srcX, ySize, box[2], box[3]);
				drawTexturedModalRect(j + box[0] + 2, k + box[1] + 2, box[2] * 2 + (box[2] - 4) * side.getId(), ySize, box[2] - 4, box[3] - 4);
				drawMouseMover(Localization.GUI.DISTRIBUTOR.SIDE.translate(side.getName()) + ((activeId != -1)
				                                                                              ? ("\n[" + Localization.GUI.DISTRIBUTOR.DROP_INSTRUCTION.translate() + "]")
				                                                                              : ""), x, y, box);
				int settingCount = 0;
				for (final DistributorSetting setting : DistributorSetting.settings) {
					if (setting.isEnabled(distributor) && side.isSet(setting.getId())) {
						final int[] settingbox = getActiveSettingBoxRect(id, settingCount++);
						drawSetting(setting, settingbox, inRect(x, y, settingbox));
						drawMouseMover(setting.getName(invs) + "\n[" + Localization.GUI.DISTRIBUTOR.REMOVE_INSTRUCTION.translate() + "]", x, y, settingbox);
					}
				}
				++id;
			}
		}
		for (final DistributorSetting setting2 : DistributorSetting.settings) {
			if (setting2.isEnabled(distributor)) {
				final int[] box = getSettingBoxRect(setting2.getImageId(), setting2.getIsTop());
				drawSetting(setting2, box, inRect(x, y, box));
				drawMouseMover(setting2.getName(invs), x, y, box);
			}
		}
		if (activeId != -1) {
			final DistributorSetting setting3 = DistributorSetting.settings.get(activeId);
			drawSetting(setting3, new int[] { x - 8, y - 8, 16, 16 }, true);
		}
	}

	private void drawSetting(final DistributorSetting setting, final int[] box, final boolean hover) {
		final int j = getGuiLeft();
		final int k = getGuiTop();
		int srcX = 0;
		if (!setting.getIsTop()) {
			srcX += box[2] * 2;
		}
		if (hover) {
			srcX += box[2];
		}
		drawTexturedModalRect(j + box[0], k + box[1], srcX, ySize + getSideBoxRect(0)[3], box[2], box[3]);
		drawTexturedModalRect(j + box[0] + 1, k + box[1] + 1, box[2] * 4 + (box[2] - 2) * setting.getImageId(), ySize + getSideBoxRect(0)[3], box[2] - 2, box[3] - 2);
	}

	private int[] getSideBoxRect(final int i) {
		return new int[] { 20, 18 + i * 24, 22, 22 };
	}

	private int[] getSettingBoxRect(final int i, final boolean topRow) {
		return new int[] { 20 + i * 18, 143 + (topRow ? 0 : 18), 16, 16 };
	}

	private int[] getActiveSettingBoxRect(final int side, final int setting) {
		final int[] sideCoords = getSideBoxRect(side);
		return new int[] { sideCoords[0] + sideCoords[2] + 5 + setting * 18, sideCoords[1] + (sideCoords[3] - 16) / 2, 16, 16 };
	}

	@Override
	public void mouseClick(int x, int y, final int button) {
		super.mouseClick(x, y, button);
		x -= getGuiLeft();
		y -= getGuiTop();
		if (button == 0) {
			for (final DistributorSetting setting : DistributorSetting.settings) {
				if (setting.isEnabled(distributor)) {
					final int[] box = getSettingBoxRect(setting.getImageId(), setting.getIsTop());
					if (!inRect(x, y, box)) {
						continue;
					}
					activeId = setting.getId();
				}
			}
		}
	}

	@Override
	public void mouseMoved(int x, int y, final int button) {
		super.mouseMoved(x, y, button);
		x -= getGuiLeft();
		y -= getGuiTop();
		if (button == 0 && activeId != -1) {
			int id = 0;
			for (final DistributorSide side : distributor.getSides()) {
				if (side.isEnabled(distributor)) {
					final int[] box = getSideBoxRect(id++);
					if (inRect(x, y, box)) {
						distributor.sendPacket(0, o -> {
							o.writeByte((byte) activeId);
							o.writeByte((byte) side.getId());
						});
						break;
					}
				}
			}
			activeId = -1;
		} else if (button == 1) {
			int id = 0;
			for (final DistributorSide side : distributor.getSides()) {
				if (side.isEnabled(distributor)) {
					int settingCount = 0;
					for (final DistributorSetting setting : DistributorSetting.settings) {
						if (setting.isEnabled(distributor) && side.isSet(setting.getId())) {
							final int[] settingbox = getActiveSettingBoxRect(id, settingCount++);
							if (!inRect(x, y, settingbox)) {
								continue;
							}
							distributor.sendPacket(1, o -> {
								o.writeByte((byte) setting.getId());
								o.writeByte((byte) side.getId());
							});
						}
					}
					++id;
				}
			}
		}
	}

	static {
		GuiDistributor.texture = ResourceHelper.getResource("/gui/distributor.png");
	}
}
