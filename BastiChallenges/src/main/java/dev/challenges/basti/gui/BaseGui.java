package dev.challenges.basti.gui;

import dev.challenges.basti.BastiPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Basisklasse aller GUIs. Der InventoryHolder dient zur Erkennung im InventoryClickListener. */
public abstract class BaseGui implements InventoryHolder {
    protected final BastiPlugin plugin;
    protected Inventory inv;

    protected BaseGui(BastiPlugin plugin) {
        this.plugin = plugin;
    }

    protected void create(int size, Component title) {
        this.inv = Bukkit.createInventory(this, size, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    /** Klick im OBEREN Inventar (das Event ist bereits abgebrochen). */
    public abstract void onClick(InventoryClickEvent e);
}
