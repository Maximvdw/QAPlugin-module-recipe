package be.maximvdw.qaplugin.modules.utils;

import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LocaleDownloader {
    /**
     * Minecraft - Versions URL
     */
    public static String URL_VERSIONS = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    /**
     * Minecraft - Indexes URL
     */
    public static String URL_INDEXES = "https://s3.amazonaws.com/Minecraft.Download/indexes/";
    /**
     * Minecraft - Resources URL
     */
    public static String URL_RESOURCES = "http://resources.download.minecraft.net/";

    private MinecraftLanguage defaultLanguage = null;

    public LocaleDownloader() {
        getLanguages();
    }


    public List<MinecraftLanguage> getLanguages() {
        // Initialize the output
        List<MinecraftLanguage> languages = new ArrayList<MinecraftLanguage>();
        try {
            JSONParser parser = new JSONParser();
            // Step 1) Get the server version
            String version = BukkitUtils.getVersionMajor() + "." + BukkitUtils.getVersionMinor();
            if (BukkitUtils.getVersionBuild() != 0) {
                version += "." + BukkitUtils.getVersionBuild();
            }

            // Step 2) We need to get the assets version of the server
            // This is because for example: 1.11.2 needs asset version 1.11
            // but 1.7.10 needs asset version 1.7.10 , as a server we don't know
            // if we need to use the "build" in the version
            String versionsJsonString = HtmlUtils.getHtmlSource(URL_VERSIONS);
            JSONObject manifestJson = (JSONObject) parser.parse(versionsJsonString);
            JSONArray versionsArray = (JSONArray) manifestJson.get("versions");
            String versionURL = null;
            for (Object versionObject : versionsArray) {
                JSONObject versionJsonObject = ((JSONObject) versionObject);
                if (versionJsonObject.get("id").equals(version)) {
                    versionURL = (String) versionJsonObject.get("url");
                    break;
                }
            }
            if (versionURL == null) {
                // Not found
                return languages;
            }
            String versionJsonString = HtmlUtils.getHtmlSource(versionURL);
            JSONObject versionJson = (JSONObject) parser.parse(versionJsonString);
            String assetsVersion = (String) versionJson.get("assets");

            // Step 3) Get the index containing all assets for the client
            String indexJson = HtmlUtils
                    .getHtmlSource(URL_INDEXES
                            + assetsVersion + ".json");
            JSONObject nodes = (JSONObject) parser.parse(indexJson);
            JSONObject objects = (JSONObject) nodes.get("objects");
            for (Object entry : objects.keySet()) {
                // Look for an asset with minecraft/lang
                // There are also assets with "lang" for REALMS, but we only
                // need the ones for minecraft
                if (String.valueOf(entry).contains("minecraft/lang")) {
                    String language = String.valueOf(entry).substring(String.valueOf(entry).lastIndexOf("/") + 1).replace(".lang", "");
                    String hash = (String) ((JSONObject) objects.get(entry)).get("hash");
                    String resourceURL = URL_RESOURCES + hash.substring(0, 2) + "/" + hash;
                    String languageProp = HtmlUtils
                            .getHtmlSource(resourceURL);
                    Properties prop = new Properties();
                    prop.load(new StringReader(languageProp));
                    MinecraftLanguage minecraftLanguage = new MinecraftLanguage(language, prop);
                    languages.add(minecraftLanguage);

                    if (minecraftLanguage.getLanguage().equalsIgnoreCase("en_gb")) {
                        setDefaultLanguage(minecraftLanguage);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return languages; // Return the languages list
    }

    /**
     * Get translation key for input
     *
     * @param name  display name in english
     * @param exact Exact match
     * @return translation key
     */
    public String getTranslationKey(String name, boolean exact) {
        return defaultLanguage.getTranslationKey(name, exact);
    }

    public MinecraftLanguage getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(MinecraftLanguage defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public static class MinecraftLanguage {
        private String language = "";
        private Properties properties = null;

        public MinecraftLanguage() {

        }

        public MinecraftLanguage(String language, Properties properties) {
            setLanguage(language);
            setProperties(properties);
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        /**
         * Get translation key for input
         *
         * @param name  display name in english
         * @param exact Exact match
         * @return translation key
         */
        public String getTranslationKey(String name, boolean exact) {
            Properties properties = getProperties();
            for (Map.Entry<Object, Object> prop : properties.entrySet()) {
                if (exact) {
                    if (String.valueOf(prop.getValue()).equals(name)) {
                        return (String) prop.getKey();
                    }
                } else {
                    if (String.valueOf(prop.getValue()).equalsIgnoreCase(name)) {
                        return (String) prop.getKey();
                    }
                }
            }
            return null;
        }

        /**
         * Get translation key
         *
         * @param item item to translate
         * @return translation key
         */
        public String getTranslationKey(ItemStack item) {
            return getTranslationKey(item.getItemMeta().getDisplayName(), false);
        }
    }
}
