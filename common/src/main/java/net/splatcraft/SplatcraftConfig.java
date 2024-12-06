package net.splatcraft;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;

import java.io.File;
import java.util.Objects;
import java.util.function.Predicate;

public interface SplatcraftConfig
{
    ForgeConfigSpec clientConfig;
    ForgeConfigSpec serverConfig;

    static
    {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();

        Client.init(configBuilder);
        clientConfig = configBuilder.build();

        configBuilder = new ForgeConfigSpec.Builder();
        Server.init(configBuilder);
        serverConfig = configBuilder.build();
    }

    static void loadConfig(ForgeConfigSpec config, String path)
    {
        final CommentedFileConfig file = CommentedFileConfig.builder(new File(path)).sync().autosave().writingMode(WritingMode.REPLACE).build();

        file.load();
        config.setConfig(file);
    }

    enum InkIndicator
    {
        CROSSHAIR,
        DURABILITY,
        BOTH,
        NONE
    }

    enum PreventBobView
    {
        SUBMERGED,
        ALWAYS,
        OFF
    }

    class Server
    {
        public static ForgeConfigSpec.BooleanValue limitFallSpeed;

        public static void init(ForgeConfigSpec.Builder client)
        {
            client.comment("Server Settings");
            limitFallSpeed = client.comment("Specifies whether to limit the maximum fall speed of players").define("splatcraft.limitFallSpeed", false);
        }
    }

    class Client
    {
        public static ForgeConfigSpec.EnumValue<SplatcraftKeyHandler.KeyMode> squidKeyMode;
        public static Setting<InkIndicator> inkIndicator;
        public static Setting<Boolean> vanillaInkDurability;
        public static Setting<Boolean> holdBarrierToRender;
        public static Setting<Integer> barrierRenderDistance;
        public static Setting<Double> inkTankGuiScale;
        public static Setting<Boolean> colorLock;
        public static Setting<Boolean> makeShinier;
        public static Setting<PreventBobView> preventBobView;
        public static Setting<Boolean> lowInkWarning;
        public static Setting<Boolean> coloredPlayerNames;
        public static String inkColoredSkinLayerPath = "config\\splatcraft\\player_ink_color.png";
        public static File playerSkinInkColoredLayer;

        public static void init()
        {
            squidKeyMode = client.comment("Squid Key Mode").defineEnum("splatcraft.squidKeyMode", SplatcraftKeyHandler.KeyMode.TOGGLE);
            inkIndicator = new Setting<>(InkIndicator.BOTH, "Determines how the amount of ink left in your tank is visualized.", "splatcraft.inkIndicator");
            vanillaInkDurability = new Setting<>(false, "Determines whether the indicator that determines how much ink you have left matches vanilla durability colors instead of your ink color.", "splatcraft.vanillaInkDurabilityColor");
            holdBarrierToRender = client.comment("Prevents Stage Barriers from rendering in creative mode unless the player is holding one in their hand.")
                .define("splatcraft.holdBarrierToRender", true);
            barrierRenderDistance = client.comment("How far away stage barriers or voids will render away from you.")
                .defineInRange("splatcraft.barrierRenderDistance", 40, 4, 80);
            colorLock = client.comment("Sets a static color for friendly and hostile colors").define("splatcraft.colorLock", false);
            makeShinier = client.comment("Makes projectiles colors more palid and more \"resistant\" to lightning changes").define("splatcraft.shinierConfig", false);
            preventBobView = client.comment("Prevents changing FOV when in Squid Mode").defineEnum("splatcraft.preventBobView", PreventBobView.OFF);
            lowInkWarning = client.comment("Determines whether the ink indicator near your crosshair warns you if your ink is low.")
                .define("splatcraft.lowInkWarning", true);
            coloredPlayerNames = client.comment("Determines whether instances of player names share the same color as their ink.")
                .define("splatcraft.coloredPlayerNames", true);
            inkTankGuiScale = client.comment("Specifies the scale of the GUI that is associated with the ink tank.").defineInRange("splatcraft.inkTankGuiScale", 1f, 0.1f, 10f);
        }
    }

    final class Setting<S>
    {
        private final S defaultValue;
        private final String description;
        private final String configKey;
        private final Predicate<S> checker;
        private S value;

        public Setting(S defaultValue, String description, String configKey, Predicate<S> checker)
        {
            this.defaultValue = defaultValue;
            this.description = description;
            this.configKey = configKey;
            this.checker = checker;
        }

        public Setting(S defaultValue, String description, String configKey)
        {
            this(defaultValue, description, configKey, null);
        }

        public boolean setValue(S newValue)
        {
            boolean passedCheck = checker == null || checker.test(newValue);
            if (passedCheck)
            {
                value = newValue;
            }
            return passedCheck;
        }

        public boolean get()
        {
            return false;
        }

        public S defaultValue()
        {
            return defaultValue;
        }

        public String description()
        {
            return description;
        }

        public String configKey()
        {
            return configKey;
        }

        public Predicate<S> checker()
        {
            return checker;
        }

        @Override
        public String toString()
        {
            return "Setting[" +
                "defaultValue=" + defaultValue + ", " +
                "description=" + description + ", " +
                "configKey=" + configKey + ", " +
                "checker=" + checker + ']';
        }

        public S getValue()
        {
            return value;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof Setting<?> setting)) return false;
            return Objects.equals(defaultValue, setting.defaultValue) && Objects.equals(description, setting.description) && Objects.equals(configKey, setting.configKey) && Objects.equals(checker, setting.checker) && Objects.equals(value, setting.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(defaultValue, description, configKey, checker, value);
        }
    }
}
