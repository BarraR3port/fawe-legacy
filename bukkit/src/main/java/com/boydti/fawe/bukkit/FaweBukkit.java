package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.bukkit.chat.BukkitChatManager;
import com.boydti.fawe.bukkit.listener.BrushListener;
import com.boydti.fawe.bukkit.listener.RenderListener;
import com.boydti.fawe.bukkit.regions.FreeBuildRegion;
import com.boydti.fawe.bukkit.regions.Worldguard;
import com.boydti.fawe.bukkit.util.BukkitReflectionUtils;
import com.boydti.fawe.bukkit.util.BukkitTaskMan;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.bukkit.v0.BukkitQueue_All;
import com.boydti.fawe.bukkit.v0.ChunkListener_9;
import com.boydti.fawe.bukkit.v1_10.BukkitQueue_1_10;
import com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11;
import com.boydti.fawe.bukkit.v1_12.BukkitQueue_1_12;
import com.boydti.fawe.bukkit.v1_12.NMSRegistryDumper;
import com.boydti.fawe.bukkit.v1_8.BukkitQueue18R3;
import com.boydti.fawe.bukkit.v1_9.BukkitQueue_1_9_R1;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.cui.CUI;
import com.boydti.fawe.util.image.ImageViewer;
import com.sk89q.bukkit.util.FallbackRegistrationListener;
import com.sk89q.worldedit.bukkit.BukkitPlayerBlockBag;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.EditSessionBlockChangeDelegate;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FaweBukkit implements IFawe, Listener {

    private final BukkitMain plugin;
    private WorldEditPlugin worldedit;
    private ItemUtil itemUtil;

    public WorldEditPlugin getWorldEditPlugin() {
        if (this.worldedit == null) {
            this.worldedit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        }
        return this.worldedit;
    }

    public FaweBukkit(BukkitMain plugin) {
        this.plugin = plugin;
        try {
            Settings.IMP.TICK_LIMITER.ENABLED = !Bukkit.hasWhitelist();
            Fawe.set(this);
            setupInjector();
            try {
                com.sk89q.worldedit.bukkit.BukkitPlayer.inject(); // Fixes
                BukkitWorld.inject(); // Fixes
                BukkitPlayerBlockBag.inject(); // features
                try {
                    FallbackRegistrationListener.inject(); // Fixes
                } catch (Throwable ignore) {} // Not important at all
            } catch (Throwable e) {
                debug("========= INJECTOR FAILED =========");
                e.printStackTrace();
                debug("===================================");
            }
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                debug("====== BRUSH LISTENER FAILED ======");
                e.printStackTrace();
                debug("===================================");
            }
            if (Bukkit.getVersion().contains("git-Spigot")) {
                debug("====== USE PAPER ======");
                debug("DOWNLOAD: https://ci.destroystokyo.com/job/PaperSpigot/");
                debug("GUIDE: https://www.spigotmc.org/threads/21726/");
                debug(" - This is only a recommendation");
                debug("==============================");
            }
            if (Bukkit.getVersion().contains("git-Paper") && Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING > 1) {
                new RenderListener(plugin);
            }
            try {
                Fawe.get().setChatManager(new BukkitChatManager());
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        } catch (final Throwable e) {
            MainUtil.handleError(e);
            Bukkit.getServer().shutdown();
        }

        // Registered delayed Event Listeners
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                // This class
                Bukkit.getPluginManager().registerEvents(FaweBukkit.this, FaweBukkit.this.plugin);

                new ChunkListener_9();
            }
        });
    }

    @Override
    public CUI getCUI(FawePlayer player) {
        return null;
    }

    @Override
    public void registerPacketListener() {
    }

    @Override
    public ImageViewer getImageViewer(FawePlayer fp) {
        return null;
    }

    @Override
    public int getPlayerCount() {
        return plugin.getServer().getOnlinePlayers().size();
    }

    @Override
    public boolean isOnlineMode() {
        return Bukkit.getOnlineMode();
    }

    @Override
    public String getPlatformVersion() {
        String bukkitVersion = Bukkit.getVersion();
        int index = bukkitVersion.indexOf("MC: ");
        return index == -1 ? bukkitVersion : bukkitVersion.substring(index + 4, bukkitVersion.length() - 1);
    }

    public void setupInjector() {
        Fawe.setupInjector();
        // Inject
        EditSessionBlockChangeDelegate.inject();
    }

    @Override
    public void debug(final String s) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        if (console != null) {
            console.sendMessage(BBC.color(s));
        } else {
            Bukkit.getLogger().info(BBC.color(s));
        }
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public void setupCommand(final String label, final FaweCommand cmd) {
        plugin.getCommand(label).setExecutor(new BukkitCommand(cmd));
    }

    @Override
    public FawePlayer<Player> wrap(final Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            Player player = Bukkit.getPlayer(name);
            return player != null ? new BukkitPlayer(player) : null;
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new BukkitPlayer(player);
        } else if (obj != null && obj.getClass().getName().contains("EntityPlayer")) {
            try {
                Method method = obj.getClass().getDeclaredMethod("getBukkitEntity");
                return wrap(method.invoke(obj));
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public ItemUtil getItemUtil() {
        ItemUtil tmp = itemUtil;
        if (tmp == null) {
            try {
                this.itemUtil = tmp = new ItemUtil();
            } catch (Throwable e) {
                Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES = false;
                debug("===== PERSISTENT BRUSH FAILED =====");
                e.printStackTrace();
                debug("===================================");
            }
        }
        return tmp;
    }

    /**
     * Vault isn't required, but used for setting player permissions (WorldEdit bypass)
     * @return
     */
    @Override
    public void setupVault() {
    }

    @Override
    public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();
        List<String> pl = new ArrayList<>();
        msg.append("Server Version: " + Bukkit.getVersion() + "\n");
        msg.append("Plugins: \n");
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            msg.append(" - " + p.getName() + ": " + p.getDescription().getVersion() + "\n");
        }
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks
     */
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskMan(plugin);
    }

    private boolean hasNMS = true;
    private boolean playerChunk = false;

    @Override
    public FaweQueue getNewQueue(String world, boolean fast) {
        if (playerChunk != (playerChunk = true)) {
            try {
                Field fieldDirtyCount = BukkitReflectionUtils.getRefClass("{nms}.PlayerChunk").getField("dirtyCount").getRealField();
                fieldDirtyCount.setAccessible(true);
                int mod = fieldDirtyCount.getModifiers();
                if ((mod & Modifier.VOLATILE) == 0) {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(fieldDirtyCount, mod + Modifier.VOLATILE);
                }
            } catch (Throwable ignore) {}
        }
        try {
            return getQueue(world);
        } catch (Throwable ignore) {
            // Disable incompatible settings
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1; // BukkitAPI placer is too slow to parallel thread at the chunk level
            Settings.IMP.HISTORY.COMBINE_STAGES = false; // Performing a chunk copy (if possible) wouldn't be faster using the BukkitAPI
            if (hasNMS) {

                debug("====== NO NMS BLOCK PLACER FOUND ======");
                debug("FAWE couldn't find a fast block placer");
                debug("Bukkit version: " + Bukkit.getVersion());
                debug("NMS label: " + plugin.getClass().getSimpleName().split("_")[1]);
                debug("Fallback placer: " + BukkitQueue_All.class);
                debug("=======================================");
                debug("Download the version of FAWE for your platform");
                debug(" - http://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/target");
                debug("=======================================");
                ignore.printStackTrace();
                debug("=======================================");
                TaskManager.IMP.laterAsync(new Runnable() {
                    @Override
                    public void run() {
                        MainUtil.sendAdmin("&cNo NMS placer found, see console!");
                    }
                }, 1);
                hasNMS = false;
            }
            return new BukkitQueue_All(world);
        }
    }

    /**
     * The FaweQueue is a core part of block placement<br>
     *  - The queue returned here is used in the SetQueue class (SetQueue handles the implementation specific queue)<br>
     *  - Block changes are grouped by chunk (as it's more efficient for lighting/packet sending)<br>
     *  - The FaweQueue returned here will provide the wrapper around the chunk object (FaweChunk)<br>
     *  - When a block change is requested, the SetQueue will first check if the chunk exists in the queue, or it will create and add it<br>
     */
    @Override
    public FaweQueue getNewQueue(World world, boolean fast) {
        if (fast) {
            if (playerChunk != (playerChunk = true)) {
                try {
                    Field fieldDirtyCount = BukkitReflectionUtils.getRefClass("{nms}.PlayerChunk").getField("dirtyCount").getRealField();
                    fieldDirtyCount.setAccessible(true);
                    int mod = fieldDirtyCount.getModifiers();
                    if ((mod & Modifier.VOLATILE) == 0) {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(fieldDirtyCount, mod + Modifier.VOLATILE);
                    }
                } catch (Throwable ignore) {
                }
            }
            Throwable error = null;
            try {
                return getQueue(world);
            } catch (Throwable ignore) {
                error = ignore;
            }
            // Disable incompatible settings
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1; // BukkitAPI placer is too slow to parallel thread at the chunk level
            Settings.IMP.HISTORY.COMBINE_STAGES = false; // Performing a chunk copy (if possible) wouldn't be faster using the BukkitAPI
            if (hasNMS) {
                debug("====== NO NMS BLOCK PLACER FOUND ======");
                debug("FAWE couldn't find a fast block placer");
                debug("Bukkit version: " + Bukkit.getVersion());
                debug("NMS label: " + plugin.getClass().getSimpleName());
                debug("Fallback placer: " + BukkitQueue_All.class);
                debug("=======================================");
                debug("Download the version of FAWE for your platform");
                debug(" - http://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/target");
                debug("=======================================");
                error.printStackTrace();
                debug("=======================================");
                TaskManager.IMP.laterAsync(new Runnable() {
                    @Override
                    public void run() {
                        MainUtil.sendAdmin("&cNo NMS placer found, see console!");
                    }
                }, 1);
                hasNMS = false;
            }
        }
        return new BukkitQueue_All(world);
    }

    public BukkitMain getPlugin() {
        return plugin;
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
    }

    /**
     * A mask manager handles region restrictions e.g. PlotSquared plots / WorldGuard regions
     */
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if ((worldguardPlugin != null) && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new Worldguard(worldguardPlugin, this));
                Fawe.debug("Plugin 'WorldGuard' found. Using it now.");
            } catch (final Throwable e) {
                MainUtil.handleError(e);
            }
        }

        if (Settings.IMP.EXPERIMENTAL.FREEBUILD) {
            try {
                managers.add(new FreeBuildRegion());
                Fawe.debug("Plugin '<internal.freebuild>' found. Using it now.");
            } catch (final Throwable e) {
                MainUtil.handleError(e);
            }
        }

        return managers;
    }
//
//    @EventHandler
//    public void onWorldLoad(WorldLoadEvent event) {
//        org.bukkit.World world = event.getWorld();
//        world.setKeepSpawnInMemory(false);
//        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
//        ChunkProviderServer provider = nmsWorld.getChunkProviderServer();
//        try {
//            Field fieldChunkLoader = provider.getClass().getDeclaredField("chunkLoader");
//            ReflectionUtils.setFailsafeFieldValue(fieldChunkLoader, provider, new FaweChunkLoader());
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();
        FawePlayer fp = Fawe.get().getCachedPlayer(name);
        if (fp != null) {
            fp.unregister();
            Fawe.get().unregister(name);
        }
    }

    @Override
    public String getPlatform() {
        return "bukkit";
    }

    @Override
    public UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public Object getBlocksHubApi() {
        return null;
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    private Version version = null;

    public Version getVersion() {
        Version tmp = this.version;
        if (tmp == null) {
            tmp = Version.NONE;
            for (Version v : Version.values()) {
                try {
                    BukkitQueue_0.checkVersion(v.name());
                    this.version = tmp = v;
                    if (tmp == Version.v1_12_R1) {
                        try {
                            System.out.println("Running 1.12 registry dumper!");
                            NMSRegistryDumper dumper = new NMSRegistryDumper(MainUtil.getFile(plugin.getDataFolder(), "extrablocks.json"));
                            dumper.run();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                } catch (IllegalStateException e) {}
            }
        }
        return tmp;
    }

    public enum Version {
        v1_8_R3,
        v1_9_R2,
        v1_10_R1,
        v1_11_R1,
        v1_12_R1,
        v1_12_R2,
        v1_13_R1,
        NONE,
    }

    private FaweQueue getQueue(World world) {
        switch (getVersion()) {
            case v1_8_R3:
                return new BukkitQueue18R3(world);
            case v1_9_R2:
                return new BukkitQueue_1_9_R1(world);
            case v1_10_R1:
                return new BukkitQueue_1_10(world);
            case v1_11_R1:
                return new BukkitQueue_1_11(world);
            case v1_12_R1:
                return new BukkitQueue_1_12(world);
            default:
            case NONE:
                return new BukkitQueue_All(world);
        }
    }

    private FaweQueue getQueue(String world) {
        switch (getVersion()) {
            case v1_8_R3:
                return new BukkitQueue18R3(world);
            case v1_9_R2:
                return new BukkitQueue_1_9_R1(world);
            case v1_10_R1:
                return new BukkitQueue_1_10(world);
            case v1_11_R1:
                return new BukkitQueue_1_11(world);
            case v1_12_R1:
                return new BukkitQueue_1_12(world);
            default:
            case NONE:
                return new BukkitQueue_All(world);
        }
    }
}
