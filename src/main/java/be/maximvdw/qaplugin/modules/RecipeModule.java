package be.maximvdw.qaplugin.modules;

import be.maximvdw.qaplugin.api.AIModule;
import be.maximvdw.qaplugin.api.AIQuestionEvent;
import be.maximvdw.qaplugin.api.QAPluginAPI;
import be.maximvdw.qaplugin.api.ai.*;
import be.maximvdw.qaplugin.api.annotations.*;
import be.maximvdw.qaplugin.api.exceptions.FeatureNotEnabled;
import be.maximvdw.qaplugin.modules.utils.HtmlUtils;
import be.maximvdw.qaplugin.modules.utils.ItemData;
import be.maximvdw.qaplugin.modules.utils.LocaleDownloader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.lang.reflect.Method;
import java.util.*;

/**
 * RecipeModule
 * Created by maxim on 10-Jan-17.
 */
@ModuleName("Recipe")
@ModuleActionName("recipe")
@ModuleAuthor("Maximvdw")
@ModuleVersion("1.3.0")
@ModuleDescription("Ask the assistant for a recipe")
@ModuleScreenshots({
        "http://i.mvdw-software.com/2017-01-13_23-00-30.png",
        "http://i.mvdw-software.com/2017-01-13_23-01-11.png",
        "http://i.mvdw-software.com/2017-01-13_23-07-18.png",
        "http://i.mvdw-software.com/2017-01-13_23-10-41.png"
})
@ModulePermalink("https://github.com/Maximvdw/QAPlugin-module-recipe")
@ModuleConstraints({
    @ModuleConstraint(type = ModuleConstraint.ContraintType.QAPLUGIN_VERSION, value = "1.10.0")
})
public class RecipeModule extends AIModule implements Listener {
    private Method asNMSCopy;
    private Method a;

    public RecipeModule() {

    }
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());

        try {
            String classPackage = "";
            String packageVersion = "";

            String[] serverPackage = Bukkit.getServer().getClass().getCanonicalName().split("\\.");
            for (int i = 0; i < serverPackage.length - 1; i++) {
                if (serverPackage[i].equalsIgnoreCase("craftbukkit")) {
                    packageVersion = serverPackage[i + 1];
                }
                classPackage = classPackage + serverPackage[i] + ".";
            }
            Class<?> craftItemStack = Class.forName(classPackage + "inventory.CraftItemStack");
            asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
            a = Class.forName("net.minecraft.server." + packageVersion + ".ItemStack").getDeclaredMethod("a");
        } catch (Exception e) {
            e.printStackTrace();
            setEnabled(false);
            return;

        }

        if (QAPluginAPI.findEntityByName("recipe-item-names") == null) {
            info("Downloading Minecraft language files to get item names ... (one time only)");
            LocaleDownloader localeDownloader = new LocaleDownloader();
            localeDownloader.getLanguages();
            Entity recipeNames = new Entity("recipe-item-names");
            Map<ItemData, List<String>> names = new HashMap<ItemData, List<String>>();
            try {
                info("Getting item data from Essentials ...");
                String source = HtmlUtils.getHtmlSource("https://raw.githubusercontent.com/drtshock/Essentials/2.x/Essentials/src/items.csv");
                String[] lines = source.split("\n");
                for (String line : lines) {
                    line = line.trim().toLowerCase(Locale.ENGLISH);
                    if (line.length() > 0 && line.charAt(0) == '#') {
                        continue;
                    }

                    final String[] parts = line.split("[^a-z0-9]");
                    if (parts.length < 2) {
                        continue;
                    }

                    final int numeric = Integer.parseInt(parts[1]);
                    String itemName = parts[0].toLowerCase(Locale.ENGLISH);
                    final short data = parts.length > 2 && !parts[2].equals("0") ? Short.parseShort(parts[2]) : 0;
                    Material m = Material.getMaterial(numeric);
                    ItemData itemData = new ItemData(m, data);
                    if (names.containsKey(itemData)) {
                        List<String> nameList = names.get(itemData);
                        nameList.add(itemName);
                    } else {
                        List<String> nameList = new ArrayList<String>();
                        nameList.add(itemName);
                        names.put(itemData, nameList);
                    }
                }
                info("Item database length: " + names.size());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();

            while (recipeIterator.hasNext()) {
                Recipe recipe = recipeIterator.next();
                ItemStack item = recipe.getResult();
                if (item.getType().equals(Material.AIR)) {
                    continue; // Why would you wanna craft air
                }
                ItemData itemData = new ItemData(item.getType(), item.getDurability());
                // Recipe available
                EntityEntry entry = new EntityEntry(itemData.getMaterial().name() + ":" + itemData.getData());
                if (itemData.getData() == 0) {
                    entry.addSynonym(itemData.getMaterial().name().toLowerCase());
                    if (itemData.getMaterial().name().contains("_")) {
                        entry.addSynonym(itemData.getMaterial().name().toLowerCase().replace("_", " "));
                    }
                }
                if (item.hasItemMeta()) {
                    entry.addSynonym(item.getItemMeta().getDisplayName());
                }
                try {
                    String translationKey = (String) a.invoke(asNMSCopy.invoke(null, item));
                    if (!translationKey.equals("tile.air")) {
                        String translation = localeDownloader.getDefaultLanguage().getProperties().getProperty(translationKey + ".name");
                        if (translation == null) {
                            translation = localeDownloader.getDefaultLanguage().getProperties().getProperty(translationKey);
                        }
                        if (translation != null) {
                            if (translation.contains("(")) {
                                translation = translation.substring(0, translation.indexOf("("));
                            }
                            entry.addSynonym(translation);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                List<String> nameList = names.get(itemData);
                String materialName = itemData.getMaterial().name().toLowerCase();
                if (nameList != null) {
                    int i = 0;
                    for (String name : nameList) {
                        if (name.equals(materialName)) {
                            entry.addSynonym(name);
                        } else if (name.endsWith(materialName)) {
                            entry.addSynonym(name.replace(materialName, " " + materialName));
                        } else if (name.startsWith(materialName)) {
                            entry.addSynonym(name.replace(materialName, materialName + " "));
                        } else {
                            entry.addSynonym(name);
                        }
                        i++;
                        if (i == 97) {
                            break;
                        }
                    }
                }
                recipeNames.addEntry(entry);
            }

            Intent question = new Intent("QAPlugin-module-recipe")
                    .withPriority(Intent.Priority.HIGH) // Entity is better in matching
                    .addTemplate(new IntentTemplate()
                            .addPart("how do you make a ")
                            .addPart(new IntentTemplate.TemplatePart("wooden door")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("how to make a ")
                            .addPart(new IntentTemplate.TemplatePart("iron axe")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("what is the crafting recipe for ")
                            .addPart(new IntentTemplate.TemplatePart("diamond legs")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("what is the recipe for ")
                            .addPart(new IntentTemplate.TemplatePart("saddle")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("whats the recipe for ")
                            .addPart(new IntentTemplate.TemplatePart("saddle")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("what's the recipe for ")
                            .addPart(new IntentTemplate.TemplatePart("saddle")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("how to create a ")
                            .addPart(new IntentTemplate.TemplatePart("flint and steel")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("give me the recipe for ")
                            .addPart(new IntentTemplate.TemplatePart("beacon")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("how do I create a ")
                            .addPart(new IntentTemplate.TemplatePart("wodden door")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("how to make a ")
                            .addPart(new IntentTemplate.TemplatePart("wodden door")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("how to craft a ")
                            .addPart(new IntentTemplate.TemplatePart("wodden door")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addTemplate(new IntentTemplate()
                            .addPart("show me the recipe for ")
                            .addPart(new IntentTemplate.TemplatePart("blue wool")
                                    .withAlias("recipe")
                                    .withMeta(recipeNames))
                            .addPart("?"))
                    .addResponse(new IntentResponse()
                            .withAction(this)
                            .addMessage(new IntentResponse.TextResponse()
                                    .addSpeechText("Here you go!")
                                    .addSpeechText("I will show it to you!")
                                    .addSpeechText("I will show you the recipe!"))
                            .addMessage(new IntentResponse.TextResponse()
                                    .addSpeechText("I was unable to find that item!")
                                    .addSpeechText("Unable to find that item!")
                                    .addSpeechText("Couldn't find that item. Can you try an rephrase it?")
                                    .addSpeechText("Couldn't find that item!"))
                            .addParameter(new IntentResponse.ResponseParameter("recipe", "$recipe")
                                    .setRequired(true)
                                    .withDataType(recipeNames)
                                    .addPrompt("For what item do you want the recipe?")
                                    .addPrompt("What is the name of the item?")
                                    .addPrompt("What is the name of the item you want a recipe for?")
                                    .addPrompt("Can you tell me the name of the item?")
                                    .addPrompt("What is the item you need the recipe for?")));

            try {
                QAPluginAPI.uploadEntity(recipeNames);
                QAPluginAPI.uploadIntent(question);
            } catch (FeatureNotEnabled ex) {
                severe("You do not have a developer access token in your QAPlugin config!");
            }
        }
    }

    @Override
    public void onDisable() {
        DisplayThread.clear();
    }

    @Override
    public void onDelete() {
        try {
            Intent intent = QAPluginAPI.findIntentByName("QAPlugin-module-nameless-report");
            if (intent != null) {
                if (!QAPluginAPI.deleteIntentById(intent.getId())) {
                    warning("Unable to delete intent!");
                }
            }
        } catch (FeatureNotEnabled ex) {
            severe("You do not have a developer access token in your QAPlugin config!");
        }
    }

    @Override
    public String getResponse(AIQuestionEvent event) {
        Map<String, String> params = event.getParameters();
        if (!params.containsKey("recipe")) {
            return event.getDefaultResponse();
        }

        String materialStr = params.get("recipe");
        if (!materialStr.contains(":")) {
            return event.getResponse(1);
        }

        String[] strData = materialStr.split(":");
        String materialName = strData[0];
        try {
            short data = Short.parseShort(strData[1]);

            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack item = new ItemStack(material);
            item.getData().setData((byte) data);

            List<Recipe> recipes = Bukkit.getServer().getRecipesFor(item);
            Player player = event.getPlayer();
            info("Showing recipe for: " + item.getType().name() + ":" + item.getData().getData() + " to player " + player.getName());
            DisplayThread.showRecipes(player, recipes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return event.getResponse(1);
        }

        return event.getResponse(0);
    }

    /**
     * On player quit
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        DisplayThread.removeUsersDisplayThread(event.getPlayer().getUniqueId());
    }

    /**
     * On player inventory click
     *
     * @param event InventoryClickEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getInventory().getTitle().contains("Recipe") && event.getCurrentItem() != null) {
            List<Recipe> recipes = Bukkit.getRecipesFor(event.getCurrentItem());
            if (recipes.size() > 0) {
                DisplayThread.showRecipes(player, recipes);
            }
            event.setCancelled(true);
        }
    }
}
