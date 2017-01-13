package be.maximvdw.qaplugin.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.util.*;

/**
 * Recipe Finder plugin for Bukkit/Spigot
 * Copyright (C) 2016 Oliver Youle
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Oliver Youle
 */
public class DisplayThread extends Thread {
    private static List<Material> fuels = new ArrayList<Material>();
    private static Map<UUID, DisplayThread> usersThreads = new Hashtable<UUID, DisplayThread>();
    private Inventory inventory;
    private List<Recipe> recipes;
    private Player player;
    private InventoryView inventoryView;

    static {
        fuels = new ArrayList<Material>();
        for (Material m : Material.values()) {
            if (m.isBurnable()) fuels.add(m);
        }
    }

    public DisplayThread(Player player) {
        this.player = player;
    }

    /**
     * Show recipes to player
     *
     * @param player  Player to show recipes to
     * @param recipes List of possible recipes
     */
    public static void showRecipes(Player player, List<Recipe> recipes) {
        if (recipes.size() > 0) {
            DisplayThread displayThread = new DisplayThread(player);
            setUsersDisplayThread(player.getUniqueId(), displayThread);
            displayThread.showRecipes(recipes);
        }
    }

    /**
     * Clear all running inventory threads
     */
    public static void clear() {
        for (UUID user : usersThreads.keySet()) {
            removeUsersDisplayThread(user);
        }
    }

    /**
     * Get user display thread
     *
     * @param user UUID of user
     * @return Display thread if found
     */
    public static DisplayThread getUsersDisplayThread(UUID user) {
        if (usersThreads.keySet().contains(user)) {
            return usersThreads.get(user);
        } else {
            return null;
        }
    }

    /**
     * Set user display thread
     *
     * @param user          UUID of user
     * @param displayThread Display thread
     */
    public static void setUsersDisplayThread(UUID user, DisplayThread displayThread) {
        removeUsersDisplayThread(user);
        usersThreads.put(user, displayThread);
    }

    /**
     * Remove user display thread
     *
     * @param user UUID of user
     */
    public static void removeUsersDisplayThread(UUID user) {
        DisplayThread thread;
        if ((thread = getUsersDisplayThread(user)) != null) {
            thread.kill();
            usersThreads.remove(user);
        }
    }

    /**
     * Show a list of recipes in the current thread
     *
     * @param recipes recipes list
     */
    public void showRecipes(List<Recipe> recipes) {
        if (recipes.get(0) instanceof FurnaceRecipe) {
            inventory = Bukkit.createInventory(player, InventoryType.FURNACE, "Recipe");
            inventory.setItem(2, sanitiseItemStack(recipes.get(0).getResult()));
        } else {
            inventory = Bukkit.createInventory(player, InventoryType.WORKBENCH, "Recipe");
            inventory.setItem(0, sanitiseItemStack(recipes.get(0).getResult()));
        }
        this.recipes = recipes;
        inventoryView = player.openInventory(inventory);
        start();
    }

    @Override
    public void run() {
        int fuelCounter = 0;
        int recipeCounter = 0;

        while (player.getOpenInventory().equals(inventoryView)) {
            Recipe recipe = recipes.get(recipeCounter);
            if (recipe instanceof FurnaceRecipe) {
                if (inventory.getType() != InventoryType.FURNACE) {
                    player.closeInventory();
                    inventory = Bukkit.createInventory(player, InventoryType.FURNACE, "recipe");
                    inventoryView = player.openInventory(inventory);
                }

                FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;
                inventoryView.setItem(0, sanitiseItemStack(furnaceRecipe.getInput()));
                inventoryView.setItem(1, new ItemStack(fuels.get(fuelCounter)));
                inventory.setItem(2, sanitiseItemStack(furnaceRecipe.getResult()));

                if (fuelCounter + 1 == fuels.size()) {
                    fuelCounter = 0;
                } else {
                    fuelCounter++;
                }
            } else {
                if (inventory.getType() != InventoryType.WORKBENCH) {
                    player.closeInventory();
                    inventory = Bukkit.createInventory(player, InventoryType.WORKBENCH, "recipe");
                    inventoryView = player.openInventory(inventory);
                }

                for (int i = 1; i < 10; i++) {
                    inventory.setItem(i, new ItemStack(Material.AIR));
                }

                if (recipe instanceof ShapedRecipe) {
                    ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                    String[] shape = shapedRecipe.getShape();
                    for (int i = 0; i < shape.length; i++) {
                        for (int x = 0; x < shape[i].length(); x++) {
                            ItemStack ingredient = sanitiseItemStack(shapedRecipe.getIngredientMap().get(shape[i].toCharArray()[x]));
                            inventoryView.setItem(i * 3 + x + 1, ingredient);
                        }
                    }
                    inventoryView.setItem(0, sanitiseItemStack(shapedRecipe.getResult()));
                } else if (recipe instanceof ShapelessRecipe) {
                    ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                    for (int i = 0; i < shapelessRecipe.getIngredientList().size(); i++) {
                        ItemStack ingredient = sanitiseItemStack(shapelessRecipe.getIngredientList().get(i));
                        inventoryView.setItem(i + 1, ingredient);
                    }
                    inventoryView.setItem(0, sanitiseItemStack(shapelessRecipe.getResult()));
                }
            }
            player.updateInventory();

            if (recipeCounter + 1 == recipes.size()) {
                recipeCounter = 0;
            } else {
                recipeCounter++;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Destroy inventory
     */
    public void kill() {
        inventoryView = null;
    }

    public ItemStack sanitiseItemStack(ItemStack brokenStack) {
        if (brokenStack != null && brokenStack.getType() != null) {
            if (brokenStack.getData() != null && brokenStack.getData().getData() < 0) {
                ItemStack newItem = new ItemStack(brokenStack.getType());
                newItem.setData(brokenStack.getData());
                newItem.setAmount(brokenStack.getAmount());
                return newItem;
            }
            return brokenStack;
        } else {
            return new ItemStack(Material.AIR);
        }
    }

}