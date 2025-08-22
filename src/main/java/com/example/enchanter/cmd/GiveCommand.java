package com.example.enchanter.cmd;

import com.example.enchanter.EnchGivePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class GiveCommand implements CommandExecutor, TabCompleter {

    private final EnchGivePlugin plugin;
    private final Map<String, String> aliasMap;

    public GiveCommand(EnchGivePlugin plugin) {
        this.plugin = plugin;
        // Common aliases → canonical keys
        Map<String,String> m = new HashMap<>();
        m.put("vanishing", "vanishing_curse");
        m.put("curseofvanishing", "vanishing_curse");
        m.put("binding", "binding_curse");
        m.put("curseofbinding", "binding_curse");
        m.put("unbreak", "unbreaking");
        m.put("silktouch", "silk_touch");
        m.put("fireaspect", "fire_aspect");
        m.put("flame", "flame");
        m.put("looting", "looting");
        m.put("fortune", "fortune");
        m.put("eff", "efficiency");
        m.put("sharp", "sharpness");
        m.put("power", "power");
        m.put("prot", "protection");
        m.put("blastprot", "blast_protection");
        m.put("projprot", "projectile_protection");
        this.aliasMap = m;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("enchanter.give") && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cYou lack permission: enchanter.give");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§eUsage: /" + label + " <player> <material> <amount> [enchant[:level] ...]");
            return true;
        }

        Player target = resolvePlayer(sender, args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        Material mat = matchMaterial(args[1]);
        if (mat == null) {
            sender.sendMessage("§cUnknown material: " + args[1]);
            return true;
        }

        int amount;
        try {
            amount = Math.max(1, Integer.parseInt(args[2]));
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number: " + args[2]);
            return true;
        }

        // Parse enchantments from args[3+]
        List<EnchantSpec> enchants = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            EnchantSpec spec = parseEnchant(args[i]);
            if (spec == null) {
                sender.sendMessage("§cUnknown enchant: " + args[i]);
                return true;
            }
            enchants.add(spec);
        }

        giveItems(target, mat, amount, enchants);
        sender.sendMessage("§aGave §f" + amount + "x " + mat.name().toLowerCase(Locale.ROOT) +
                " §ato §f" + target.getName() + formatEnchantsSuffix(enchants));
        return true;
    }

    private Player resolvePlayer(CommandSender sender, String token) {
        if ("me".equalsIgnoreCase(token) && sender instanceof Player p) return p;
        return Bukkit.getPlayerExact(token);
    }

    private Material matchMaterial(String token) {
        String t = token.toUpperCase(Locale.ROOT);
        Material m = Material.matchMaterial(t);
        if (m == null && t.startsWith("MINECRAFT:")) {
            m = Material.matchMaterial(t.substring("MINECRAFT:".length()));
        }
        return m;
    }

    private record EnchantSpec(Enchantment enchant, int level) {}

    private EnchantSpec parseEnchant(String tokenIn) {
        String token = tokenIn.toLowerCase(Locale.ROOT);

        // Split on the LAST ':' to allow namespaced keys like 'minecraft:sharpness:5'
        int lastColon = token.lastIndexOf(':');
        String namePart = token;
        int level = 1;
        if (lastColon > 0 && lastColon < token.length() - 1) {
            String tail = token.substring(lastColon + 1);
            if (tail.chars().allMatch(Character::isDigit)) {
                namePart = token.substring(0, lastColon);
                try { level = Integer.parseInt(tail); } catch (Exception ignored) {}
            }
        }

        String keyRaw = aliasMap.getOrDefault(namePart.replace('-', '_'), namePart.replace('-', '_'));

        // Try namespaced first
        Enchantment ench = null;
        NamespacedKey key = NamespacedKey.fromString(keyRaw.contains(":") ? keyRaw : "minecraft:" + keyRaw);
        if (key != null) ench = Enchantment.getByKey(key);

        // Fallback to legacy name
        if (ench == null) ench = Enchantment.getByName(keyRaw.toUpperCase(Locale.ROOT));

        return (ench == null) ? null : new EnchantSpec(ench, Math.max(1, level));
    }

    private void giveItems(Player target, Material mat, int total, List<EnchantSpec> enchants) {
        int max = mat.getMaxStackSize();
        int remaining = total;
        while (remaining > 0) {
            int take = Math.min(max, remaining);
            ItemStack stack = new ItemStack(mat, take);
            for (EnchantSpec es : enchants) {
                // Unsafe = allow anything, any level, any item
                stack.addUnsafeEnchantment(es.enchant(), es.level());
            }
            HashMap<Integer, ItemStack> leftovers = target.getInventory().addItem(stack);
            leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            remaining -= take;
        }
        target.updateInventory();
    }

    private String formatEnchantsSuffix(List<EnchantSpec> specs) {
        if (specs.isEmpty()) return "";
        return " §7with §f" + specs.stream()
                .map(s -> {
                    NamespacedKey k = s.enchant().getKey();
                    return k.getKey() + ":" + s.level();
                })
                .collect(Collectors.joining(", "));
    }

    // ---------- Tab completion ----------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("enchanter.give")) return Collections.emptyList();

        switch (args.length) {
            case 1 -> {
                String pfx = args[0].toLowerCase(Locale.ROOT);
                List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
                if ("me".startsWith(pfx)) {
                    List<String> out = new ArrayList<>();
                    out.add("me");
                    out.addAll(names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pfx)).toList());
                    return out;
                }
                return names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pfx)).toList();
            }
            case 2 -> {
                String pfx = args[1].toLowerCase(Locale.ROOT);
                return Arrays.stream(Material.values())
                        .map(m -> m.name().toLowerCase(Locale.ROOT))
                        .filter(n -> n.startsWith(pfx))
                        .limit(200)
                        .toList();
            }
            case 3 -> {
                // amount suggestions
                return Arrays.asList("1", "16", "32", "64");
            }
            default -> {
                // enchantments (with optional ":<level>")
                String pfx = args[args.length - 1].toLowerCase(Locale.ROOT);
                return Arrays.stream(Enchantment.values())
                        .map(e -> e.getKey() != null ? e.getKey().getKey() : e.getName().toLowerCase(Locale.ROOT))
                        .map(k -> k + (pfx.contains(":") ? "" : ":1"))
                        .filter(s -> s.startsWith(pfx))
                        .sorted()
                        .limit(200)
                        .toList();
            }
        }
    }
}
