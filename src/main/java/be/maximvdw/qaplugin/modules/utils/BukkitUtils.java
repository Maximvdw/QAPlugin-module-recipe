package be.maximvdw.qaplugin.modules.utils;

import org.bukkit.Bukkit;

/**
 * BukkitUtils
 * <p>
 * Bukkit server utillities
 *
 * @author Maxim Van de Wynckel (Maximvdw)
 * @version 1.0
 * @project BasePlugin
 * @site http://www.mvdw-software.be/
 */
public class BukkitUtils {
    private static String version = "";

    private static int versionMajor = -1;
    private static int versionMinor = -1;
    private static int versionBuild = 0;

    static {
        getVersion();
    }

    public static String getVersion() {
        if (Bukkit.getServer() == null) {
            return "";
        }

        if (version.equals("")) {
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            String[] data = BukkitUtils.getVersion().substring(1).split("_");
            if (NumberUtils.isInteger(data[1]) && NumberUtils.isInteger(data[0])) {
                versionMinor = Integer.parseInt(data[1]);
                versionMajor = Integer.parseInt(data[0]);
                if (data[2].startsWith("R")) {
                    versionBuild = Integer.parseInt(data[2].replace("R", "")) + 1;
                }
            }
        }
        return version;
    }

    public static void setVersion(String version) {
        BukkitUtils.version = version;
    }

    /**
     * Get minecraft server version
     * <p>
     * This will get the minecraft server version as shown in the server list.
     *
     * @return Server version
     */
    public static String getServerVersion() {
        return version;
    }


    public static int getVersionMajor() {
        if (versionMajor == -1) {
            getVersion();
        }
        return versionMajor;
    }

    public static void setVersionMajor(int versionMajor) {
        BukkitUtils.versionMajor = versionMajor;
    }

    public static int getVersionMinor() {
        if (versionMinor == -1) {
            getVersion();
        }
        return versionMinor;
    }

    public static void setVersionMinor(int versionMinor) {
        BukkitUtils.versionMinor = versionMinor;
    }

    public static int getVersionBuild() {
        return versionBuild;
    }

    public static void setVersionBuild(int versionBuild) {
        BukkitUtils.versionBuild = versionBuild;
    }
}
