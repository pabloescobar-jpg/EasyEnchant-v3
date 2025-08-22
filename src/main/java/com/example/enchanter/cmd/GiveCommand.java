package com.example.enchanter.cmd;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class GiveCommand implements CommandExecutor, TabCompleter {

    // Friendly aliases -> canonical enchant keys
    private final Map<String, String> aliasMap = new HashMap<>();

    public GiveCommand() {
        // common shortcuts
        aliasMap.put("vanishing", "vanishing_curse");
        aliasMap.put("curseofvanishing", "vanishing_curse");
        aliasMap.put("binding", "binding_curse");
        aliasMap.put("curseofbinding", "binding_curse");
        aliasMap.put("unbreak", "unbreaking");
        aliasMap.put("unbreakable", "unbreaking");
        aliasMap.put("silktouch", "silk_touch");
        aliasMap.put("fireaspect", "fire_aspect");
        aliasMap.put("blastprot", "blast_protection");
        aliasMap.put("projprot", "projectile_protection");
        aliasMap.put("featherfall", "feather_falling");
        aliasMap.put("depth", "depth_strider");
        aliasMap.put("soul", "soul_speed");
        aliasMap.put("swift", "swift_sneak");
        aliasMap.put("eff", "efficiency");
        aliasMap.put("sharp", "sharpness");
        aliasMap.put("power", "power");
        aliasMap.put("prot", "protection");
        aliasMap.put("looting3", "looting"); // people type weird stuff; still resolves
    }

    // -------- main command ----------
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("enchanter.give") && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("§cYou lack permission: enchanter.give");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§eUsage: /" + label + " <player> <material> <amount> [enchant[:lvl] ...] [name...] [&color]");
            return true;
        }

        Player target = resolvePlayer(sender, args[0]);
        if (target == null) { sender.sendMessage("§cPlayer not found: " + args[0]); return true; }

        Material mat = matchMaterial(args[1]);
        if (mat == null) { sender.sendMessage("§cUnknown material: " + args[1]); return true; }

        int amount;
        try { amount = Math.max(1, Integer.parseInt(args[2])); }
        catch (NumberFormatException e) { sender.sendMessage("§cAmount must be a number: " + args[2]); return true; }

        // --- parse enchantments first ---
        List<EnchantSpec> enchants = new ArrayList<>();
        int i = 3;
        for (; i < args.length; i++){
            EnchantSpec spec = parseEnchant(args[i]);
            if (spec == null) break;           // first non-enchant token
            enchants.add(spec);
        }

        // --- optional trailing name + color ---
        String colorToken = null;
        if (i < args.length && ColorUtil.isColorToken(args[args.length - 1])) {
            colorToken = args[args.length - 1];
            args = Arrays.copyOf(args, args.length - 1); // chop color off
        }
        String customName = (i < args.length) ? String.join(" ", Arrays.copyOfRange(args, i, args.length)) : null;

        // --- give items (with optional name + color) ---
        giveItems(target, mat, amount, enchants, customName, colorToken);

        // Essentials-like feedback (green/white, dark-red player)
        sender.sendMessage("§aGave §f" + amount + " §aof §f" + mat.name().toLowerCase(Locale.ROOT)
                + " §ato §4" + target.getName());
        return true;
    }

    // -------- helpers ----------
    private Player resolvePlayer(CommandSender sender, String token) {
        if ("me".equalsIgnoreCase(token) && sender instanceof Player p) return p;
        Player exact = Bukkit.getPlayerExact(token);
        if (exact != null) return exact;
        // fallback: case-insensitive partial if unique
        List<Player> partial = Bukkit.getOnlinePlayers().stream()
                .filter(pl -> pl.getName().toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT)))
                .toList();
        return partial.size() == 1 ? partial.get(0) : null;
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
        if (tokenIn == null || tokenIn.isEmpty()) return null;
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

        // normalize name (aliases, underscores)
        String keyRaw = aliasMap.getOrDefault(namePart.replace('-', '_'), namePart.replace('-', '_'));

        // Try namespaced first
        Enchantment ench = null;
        NamespacedKey key = NamespacedKey.fromString(keyRaw.contains(":") ? keyRaw : "minecraft:" + keyRaw);
        if (key != null) ench = Enchantment.getByKey(key);

        // Fallback legacy name (UPPER)
        if (ench == null) ench = Enchantment.getByName(keyRaw.toUpperCase(Locale.ROOT));

        return (ench == null) ? null : new EnchantSpec(ench, Math.max(1, level));
    }

    private void giveItems(Player target, Material mat, int total, List<EnchantSpec> enchants, String name, String color){
        int max = mat.getMaxStackSize();
        int remaining = total;
        while (remaining > 0){
            int take = Math.min(max, remaining);
            ItemStack stack = new ItemStack(mat, take);
            for (EnchantSpec es : enchants) stack.addUnsafeEnchantment(es.enchant(), es.level());
            if (name != null || color != null){
                var meta = stack.getItemMeta();
                String base = (name != null) ? name : (meta.hasDisplayName() ? meta.getDisplayName() : pretty(mat));
                if (color != null) base = color + base;  // color comes last in syntax, but should prefix the name
                meta.setDisplayName(ColorUtil.colorize(base));
                stack.setItemMeta(meta);
            }
            var leftovers = target.getInventory().addItem(stack);
            leftovers.values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));
            remaining -= take;
        }
        target.updateInventory();
    }

    private static String pretty(Material m){
        String s = m.name().toLowerCase(Locale.ROOT).replace('_',' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // -------- tab completion ----------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("enchanter.give")) return Collections.emptyList();

        // arg1: player
        if (args.length == 1) {
            String pfx = args[0].toLowerCase(Locale.ROOT);
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
            List<String> out = new ArrayList<>();
            if ("me".startsWith(pfx)) out.add("me");
            out.addAll(names.stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(pfx)).toList());
            return out;
        }

        // arg2: material
        if (args.length == 2) {
            String pfx = args[1].toLowerCase(Locale.ROOT);
            return Arrays.stream(Material.values())
                    .map(m -> m.name().toLowerCase(Locale.ROOT))
                    .filter(n -> n.startsWith(pfx))
                    .limit(200)
                    .toList();
        }

        // arg3: amount
        if (args.length == 3) {
            return Arrays.asList("1", "16", "32", "64");
        }

        // args >= 4
        String last = args[args.length - 1];

        // Show color suggestions when the user starts typing a color token
        if (last.startsWith("&") || last.startsWith("#")) {
            return Arrays.asList("&0","&1","&2","&3","&4","&5","&6","&7","&8","&9",
                                 "&a","&b","&c","&d","&e","&f","&k","&l","&m","&n","&o","&r",
                                 "#FFFFFF","#FFAA00","#00FFFF","#FF0000","#00FF00","#AAAAAA");
        }

        // Determine where enchantments stop to decide what to suggest
        int i = 3;
        for (; i < args.length; i++) {
            if (parseEnchant(args[i]) == null) break;
        }
        boolean stillInEnchants = (args.length - 1) < i;

        if (stillInEnchants) {
            String pfx = last.toLowerCase(Locale.ROOT);
            // use available enchant keys + legacy names, add ":1" hint if no level yet
            Set<String> keys = Arrays.stream(Enchantment.values())
                    .map(e -> e != null && e.getKey() != null ? e.getKey().getKey() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            // include a few aliases
            keys.addAll(aliasMap.values());

            return keys.stream()
                    .map(k -> k + (pfx.contains(":") ? "" : ":1"))
                    .filter(s -> s.startsWith(pfx))
                    .limit(200)
                    .toList();
        }

        // past enchants = name/color area → no strong suggestions (let the user type)
        return Collections.emptyList();
    }
}

