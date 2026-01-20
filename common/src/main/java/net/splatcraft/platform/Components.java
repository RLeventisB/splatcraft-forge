package net.splatcraft.platform;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.structs.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Components
{
	private static byte nextBitId = 1;
	private static final List<ComponentData> registeredComponents = new ArrayList<>();
	public static byte getNextBitId(ComponentData component)
	{
		registeredComponents.add(component);
		byte bit = nextBitId;
		nextBitId <<= 1;
		return bit;
	}
	public static class Bits
	{
		public static final byte ENTITY_INFO = Components.ENTITY_INFO.getBitId();
		public static final byte SQUID_INFO = Components.SQUID_INFO.getBitId();
		public static final byte WEAPON_INFO = Components.WEAPON_INFO.getBitId();
		public static final byte PLAYER_INFO = Components.PLAYER_INFO.getBitId();
		public static final byte INK_OVERLAY = Components.INK_OVERLAY.getBitId();
		public static final byte LIVING_ENTITY_INFOS = (byte) (ENTITY_INFO | SQUID_INFO | WEAPON_INFO);
		public static final byte PLAYER_INFOS = (byte) (LIVING_ENTITY_INFOS | PLAYER_INFO);
		public static final byte ALL_ENTITY = (byte) (LIVING_ENTITY_INFOS | INK_OVERLAY);
		public static final byte ALL_PLAYER = (byte) (PLAYER_INFOS | INK_OVERLAY);
		public static byte ofComponents(ComponentData... components)
		{
			byte result = 0;
			for (int i = 0; i < components.length; i++)
			{
				ComponentData component = components[i];
				if (!registeredComponents.contains(component))
					continue;

				result |= component.getBitId();
			}
			return result;
		}
		public static List<ComponentData> getComponents(byte flag)
		{
			ImmutableList.Builder<ComponentData> builder = ImmutableList.builder();
			for (ComponentData component : registeredComponents)
			{
				byte id = component.getBitId();
				if ((flag & id) == id)
				{
					builder.add(component);
				}
			}

			return builder.build();
		}
		public static <HOLDER> List<ComponentData<HOLDER, Object>> getComponents(byte flag, Class<HOLDER> holderClass)
		{
			ImmutableList.Builder<ComponentData<HOLDER, Object>> builder = ImmutableList.builder();
			for (ComponentData component : registeredComponents)
			{
				if (!holderClass.isAssignableFrom(component.getHolderClass()))
					continue;

				byte id = component.getBitId();
				if ((flag & id) == id)
				{
					builder.add(component);
				}
			}

			return builder.build();
		}
		public static <HOLDER> Iterator<ComponentData<HOLDER, Object>> getComponentIterator(byte flag, Class<HOLDER> holderClass)
		{
			return new ComponentIterator<>(flag, holderClass);
		}
		public static final class ComponentIterator<HOLDER> implements Iterator<ComponentData<HOLDER, Object>>
		{
			private byte cursor;
			private final byte componentsFlag;
			private final Class<HOLDER> holderClass;
			public ComponentIterator(byte flag, Class<HOLDER> holderClass)
			{
				componentsFlag = flag;
				this.holderClass = holderClass;
				cursor = 0;

				while ((flag & 1) != 1)
				{
					flag >>= 1;
					cursor++;
				}
			}
			@Override
			public boolean hasNext()
			{
				return componentsFlag != 0 && nextCursor() < registeredComponents.size();
			}
			@Override
			public ComponentData<HOLDER, Object> next()
			{
				ComponentData result = registeredComponents.get(cursor);
				cursor = nextCursor();
				return result;
			}
			private byte nextCursor()
			{
				byte nextCursor = cursor;
				ComponentData currentData;
				do
				{
					currentData = registeredComponents.get(nextCursor);
					nextCursor++;
				}
				while (
					(!holderClass.isAssignableFrom(currentData.getHolderClass()) ||
					 (componentsFlag & currentData.getBitId()) != currentData.getBitId()
					) && nextCursor < registeredComponents.size());
				return nextCursor;
			}
		}
	}
	public static final ComponentData<LivingEntity, EntityInfo> ENTITY_INFO;
	public static final ComponentData<LivingEntity, WeaponInfo> WEAPON_INFO;
	public static final ComponentData<LivingEntity, SquidInfo> SQUID_INFO;
	public static final ComponentData<Player, PlayerInfo> PLAYER_INFO;
	public static final ComponentData<LivingEntity, InkOverlayData> INK_OVERLAY;
	public static final ComponentData<ChunkAccess, ChunkInk> CHUNK_INK;
	public static final ComponentData<MinecraftServer, SaveInfo> SAVE_INFO;
	static
	{
		ENTITY_INFO = new ComponentData<>(
			Splatcraft.identifierOf("entity_info"), LivingEntity.class, EntityInfo.class, EntityInfo.CODEC
		);
	}
	static
	{
		WEAPON_INFO = new ComponentData<>(
			Splatcraft.identifierOf("weapon_info"), LivingEntity.class, WeaponInfo.class, WeaponInfo.CODEC
		);
	}
	static
	{
		SQUID_INFO = new ComponentData<>(
			Splatcraft.identifierOf("squid_info"), LivingEntity.class, SquidInfo.class, SquidInfo.CODEC
		);
	}
	static
	{
		PLAYER_INFO = new ComponentData<>(
			Splatcraft.identifierOf("player_info"), Player.class, PlayerInfo.class, PlayerInfo.CODEC
		);
	}
	static
	{
		INK_OVERLAY = new ComponentData<>(
			Splatcraft.identifierOf("ink_overlay_data"), LivingEntity.class, InkOverlayData.class, InkOverlayData.CODEC
		);
	}
	static
	{
		CHUNK_INK = new ComponentData<>(
			Splatcraft.identifierOf("chunk_ink"), ChunkAccess.class, ChunkInk.class, ChunkInk.CODEC
		);
	}
	static
	{
		SAVE_INFO = new ComponentData<>(
			Splatcraft.identifierOf("save_info"), MinecraftServer.class, SaveInfo.class, SaveInfo.CODEC
		);
	}
}
