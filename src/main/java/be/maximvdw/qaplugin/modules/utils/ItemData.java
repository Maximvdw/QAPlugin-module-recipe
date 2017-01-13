package be.maximvdw.qaplugin.modules.utils;

import org.bukkit.Material;

/**
 * ItemData
 * Created by maxim on 10-Jan-17.
 */
public class ItemData {
    private Material material = null;
    private short data = 0;

    public ItemData(Material material, short data) {
        setMaterial(material);
        setData(data);
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public short getData() {
        return data;
    }

    public void setData(short data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemData itemData = (ItemData) o;

        if (data != itemData.data) return false;
        return material == itemData.material;

    }

    @Override
    public int hashCode() {
        int result = material != null ? material.hashCode() : 0;
        result = 31 * result + (int) data;
        return result;
    }
}
