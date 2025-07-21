package net.splatcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.UpdateStageMarkerPacket;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.ClientUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class StageMarkerEditorScreen extends Screen
{
	public static final Component TITLE = Component.translatable("container.stage_marker");
	public static final Component RELATIVE_LABEL = Component.translatable("gui.stage_marker.offset_label");
	public static final Component MARKER_TYPE_LABEL = Component.translatable("gui.stage_marker.type_label");
	public static final Component ACTIVE_LABEL = Component.translatable("gui.stage_marker.active");
	public static final Component ZONE_SIZE_LABEL = Component.translatable("gui.stage_marker.zone_size");
	public final StageMarkerTileEntity marker;
	private EditBox[] relativePosButtons;
	private CycleButton markerTypeButton;
	private List<AbstractWidget> typeDependantWidgets = new ArrayList<>();
	private double accumulatedScrollY;
	private List<EditBox> editBoxes = new ArrayList<>();
	public StageMarkerEditorScreen(StageMarkerTileEntity marker)
	{
		super(Component.empty());
		this.marker = marker;
	}
	@Override
	protected void init()
	{
		relativePosButtons = new EditBox[] {
			createRelativeCoordinateButton(Direction.Axis.X, marker),
			createRelativeCoordinateButton(Direction.Axis.Y, marker),
			createRelativeCoordinateButton(Direction.Axis.Z, marker)
		};
		
		markerTypeButton = (CycleButton.<StageMarkerTileEntity.MarkerType>builder((type) ->
				Component.translatable("gui.stage_marker.marker_type." + type.name().toLowerCase())).withValues(StageMarkerTileEntity.MarkerType.values())
			.displayOnlyValue().withInitialValue(marker.getMarkerType())
			.create(width / 2 - 100, 90, 120, 20,
				MARKER_TYPE_LABEL,
				(button, type) ->
				{
					marker.setMarkerType(type);
					loadTypeDependantWidgets();
				}));
		addRenderableWidget(markerTypeButton);
		
		addRenderableWidget(Checkbox.builder(ACTIVE_LABEL, font).pos(width / 2 + 130, 90).selected(marker.isActive()).onValueChange((box, state) ->
		{
			marker.setActive(state);
		}).build());
		
		loadTypeDependantWidgets();
	}
	private EditBox createRelativeCoordinateButton(Direction.Axis axis, StageMarkerTileEntity marker)
	{
		Consumer<Integer> onChange = switch (axis)
		{
			case X -> value ->
			{
				marker.setOffset(new BlockPos(value, marker.getOffset().getY(), marker.getOffset().getZ()));
				marker.notifyChange();
			};
			case Y -> value ->
			{
				marker.setOffset(new BlockPos(marker.getOffset().getX(), value, marker.getOffset().getZ()));
				marker.notifyChange();
			};
			case Z -> value ->
			{
				marker.setOffset(new BlockPos(marker.getOffset().getX(), marker.getOffset().getY(), value));
				marker.notifyChange();
			};
		};
		EditBox box = createNumberBox(width / 2 - 100 + 50 * axis.ordinal(), 50, 50, 20,
			Component.literal(axis.getName() + " Offset"),
			marker.getOffset().get(axis),
			onChange);
		addRenderableWidget(box);
		
		return box;
	}
	private void loadTypeDependantWidgets()
	{
		for (AbstractWidget widget : typeDependantWidgets)
		{
			removeWidget(widget);
		}
		typeDependantWidgets.clear();
		
		switch (marker.getMarkerType())
		{
			case SPLAT_ZONE ->
			{
				for (Direction.Axis axis : Direction.Axis.values())
				{
					Consumer<Integer> onChange = switch (axis)
					{
						case X -> value ->
						{
							marker.intDatas[0] = value;
							marker.notifyChange();
						};
						case Y -> value ->
						{
							marker.intDatas[1] = value;
							marker.notifyChange();
						};
						case Z -> value ->
						{
							marker.intDatas[2] = value;
							marker.notifyChange();
						};
					};
					
					typeDependantWidgets.add(createNumberBox(width / 2 - 100 + 50 * axis.ordinal(), 130, 40, 20,
						Component.literal("Splat zone " + axis.name() + " size"), marker.intDatas[axis.ordinal()], onChange));
				}
			}
			case RAINMAKER_SPAWN ->
			{
			}
			case RAINMAKER_PODIUM ->
			{
			}
			case CLAM_BASKET ->
			{
			}
			case CLAM_SPAWN_AREA ->
			{
			}
		}
		
		for (AbstractWidget widget : typeDependantWidgets)
		{
			addRenderableWidget(widget);
		}
		
		updateEditBoxList();
	}
	public void updateEditBoxList()
	{
		editBoxes.clear();
		for (GuiEventListener child : children())
		{
			if (child instanceof EditBox box)
			{
				editBoxes.add(box);
			}
		}
	}
	@Override
	public void tick()
	{
		if (marker.isRemoved() || !marker.hasLevel() || !(marker.getLevel().getBlockEntity(marker.getBlockPos()) instanceof StageMarkerTileEntity))
		{
			ClientUtils.getClient().setScreen(null);
		}
		
		boolean resetScroll = true;
		for (EditBox box : editBoxes)
		{
			if (box.isHovered())
			{
				String text = box.getValue();
				int value = tryGetInt(text).orElse(0);
				if (Math.abs(accumulatedScrollY) >= 1)
				{
					int steps = (int) accumulatedScrollY;
					value += steps;
					accumulatedScrollY -= steps;
					
					box.setValue(Integer.toString(value));
				}
				
				resetScroll = false;
				
				break;
			}
		}
		
		if (resetScroll)
			accumulatedScrollY = 0;
	}
	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		guiGraphics.drawCenteredString(font, TITLE, width / 2, 20, 16777215);
		guiGraphics.drawCenteredString(font, RELATIVE_LABEL, width / 2 - 50, 40, 16777215);
		guiGraphics.drawCenteredString(font, MARKER_TYPE_LABEL, width / 2 - 50, 80, 16777215);
		
		switch (marker.getMarkerType())
		{
			case SPLAT_ZONE ->
			{
				guiGraphics.drawCenteredString(font, ZONE_SIZE_LABEL, width / 2 - 50, 120, 16777215);
			}
			case RAINMAKER_SPAWN ->
			{
			}
			case RAINMAKER_PODIUM ->
			{
			}
			case CLAM_BASKET ->
			{
			}
			case CLAM_SPAWN_AREA ->
			{
			}
		}
		
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
	@Override
	public void removed()
	{
		SplatcraftPacketHandler.sendToServer(new UpdateStageMarkerPacket(marker));
	}
	@Override
	public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
	}
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
	{
		accumulatedScrollY += scrollY;
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
	private EditBox createNumberBox(int x, int y, int width, int height, Component narratorComponent, int defaultValue, Consumer<Integer> onChange)
	{
		EditBox box = new EditBox(font, x, y, width, height, narratorComponent);
		box.setValue(Integer.toString(defaultValue));
		box.setResponder(v ->
		{
			tryGetInt(v).ifPresentOrElse(value ->
			{
				box.setTextColor(0xFFFFFFFF);
				onChange.accept(value);
			}, () -> box.setTextColor(0xFFFF0000));
		});
		return box;
	}
	public static OptionalInt tryGetInt(String text)
	{
		try
		{
			return OptionalInt.of(Integer.decode(text));
		}
		catch (NumberFormatException e)
		{
			return OptionalInt.empty();
		}
	}
}
