package com.example.enchanter.cmd;

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

    private final Map<String, String> aliasMap = new HashMap<>();

    public GiveCommand() {
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
        aliasMap.put("looting3", "looting");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // OPs or explicitly granted players (console allowed)
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp() && !sender.hasPermission("enchanter.give")) {
            sender.sendMessage("§6[EasyEnchant] §4You must be OP or have enchanter.give.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§6Usage: /" + label + " <player> <material> [amount] [enchant[:lvl] ...] [name...] [&color]");
            return true;
        }

        Player target = resolvePlayer(sender, args[0]);
        if (target == null) { sender.sendMessage("§6[EasyEnchant] §4Player not found: §4" + args[0]); return true; }

        Material mat = matchMaterial(args[1]);
        if (mat == null) { sender.sendMessage("§6[EasyEnchant] §4Unknown material: §4" + args[1]); return true; }

        // ----- amount is OPTIONAL; default 1 if not provided or not a number -----
        int amount = 1;
        int i = 2; // index of next token after <material>
        if (args.length >= 3 && isInt(args[2])) {
            try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
            i = 3;
        }

        // --- parse enchantments first ---
        List<EnchantSpec> enchants = new ArrayList<>();
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
        String customName = (i < args.length) ? ColorUtil.normalizeSpaces(String.join(" ", Arrays.copyOfRange(args, i, args.length))) : null;

        // --- give items (with optional name + color) ---
        giveItems(target, mat, amount, enchants, customName, colorToken);

        // Essentials-style GLOBAL broadcast: orange text, dark-red fill-ins
        Bukkit.getServer().broadcastMessage(
                "§6Gave §4" + amount +
                " §6of §4" + mat.name().toLowerCase(java.util.Locale.ROOT) +
                " §6to §4" + target.getName()
        );
        return true;
    }

    private boolean isInt(String s){
        for (int k=0;k<s.length();k++) if (!Character.isDigit(s.charAt(k))) return false;
        return !s.isEmpty();
    }

    private Player resolvePlayer(CommandSender sender, String token) {
        if ("me".equalsIgnoreCase(token) && sender instanceof Player p) return p;
        Player exact = Bukkit.getPlayerExact(token);
        if (exact != null) return exact;
        String lower = token.toLowerCase(java.util.Locale.ROOT);
        Player match = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(java.util.Locale.ROOT).startsWith(lower)) {
                if (match != null) return null;
                match = p;
            }
        }
        return match;
    }

    private Material matchMaterial(String token) {
        String t = token.toUpperCase(java.util.Locale.ROOT);
        Material m = Material.matchMaterial(t);
        if (m == null && t.startsWith("MINECRAFT:")) {
            m = Material.matchMaterial(t.substring("MINECRAFT:".length()));
        }
        return m;
    }

    private record EnchantSpec(Enchantment enchant, int level) {}

    private EnchantSpec parseEnchant(String tokenIn) {
        if (tokenIn == null || tokenIn.isEmpty()) return null;
        String token = tokenIn.toLowerCase(java.util.Locale.ROOT);

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

        Enchantment ench = null;
        NamespacedKey key = NamespacedKey.fromString(keyRaw.contains(":") ? keyRaw : "minecraft:" + keyRaw);
        if (key != null) ench = Enchantment.getByKey(key);
        if (ench == null) ench = Enchantment.getByName(keyRaw.toUpperCase(java.util.Locale.ROOT));

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
                String baseName = (name != null) ? name : pretty(mat);

                // If user typed color BEFORE name (e.g., "&2 Randy Axe"), remove the space after the color code.
                String legacy = (color != null ? color : "") + baseName;
                legacy = ColorUtil.stripSpaceAfterLeadingColor(legacy);
                legacy = ColorUtil.colorize(ColorUtil.normalizeSpaces(legacy));

                meta.displayName(TextUtil.legacyNoItalics(legacy));
                stack.setItemMeta(meta);
            }
            var leftovers = target.getInventory().addItem(stack);
            leftovers.values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));
            remaining -= take;
        }
        target.updateInventory();
    }

    private static String pretty(Material m){
        String s = m.name().toLowerCase(java.util.Locale.ROOT).replace('_',' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp() && !sender.hasPermission("enchanter.give")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String pfx = args[0].toLowerCase(java.util.Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                String n = p.getName();
                if (n.toLowerCase(java.util.Locale.ROOT).startsWith(pfx)) names.add(n);
            }
            if ("me".startsWith(pfx)) names.add(0, "me");
            Collections.sort(names);
            return names;
        }

        if (args.length == 2) {
            String pfx = args[1].toLowerCase(java.util.Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Material m : Material.values()) {
                String n = m.name().toLowerCase(java.util.Locale.ROOT);
                if (n.startsWith(pfx)) {
                    out.add(n);
                    if (out.size() >= 200) break;
                }
            }
            return out;
        }

        // arg3 MAY be amount; if user is typing digits, suggest common amounts
        if (args.length == 3 && isInt(args[2])) return Arrays.asList("1", "16", "32", "64");

        String last = args[args.length - 1];
        if (last.startsWith("&") || last.startsWith("#")) {
            return Arrays.asList("&0","&1","&2","&3","&4","&5","&6","&7","&8","&9",
                                 "&a","&b","&c","&d","&e","&f","&k","&l","&m","&n","&o","&r",
                                 "#FFFFFF","#FFAA00","#00FFFF","#FF0000","#00FF00","#AAAAAA");
        }

        int i = 2;
        if (args.length >= 3 && isInt(args[2])) i = 3;
        for (; i < args.length; i++) if (parseEnchant(args[i]) == null) break;
        boolean stillInEnchants = (args.length - 1) < i;

        if (stillInEnchants) {
            String pfx = last.toLowerCase(java.util.Locale.ROOT);
            Set<String> keys = new TreeSet<>();
            for (Enchantment e : Enchantment.values()) {
                if (e != null && e.getKey() != null) keys.add(e.getKey().getKey());
            }
            keys.addAll(aliasMap.values());
            List<String> out = new ArrayList<>();
            for (String k : keys) {
                String s = k + (pfx.contains(":") ? "" : ":1");
                if (s.startsWith(pfx)) {
                    out.add(s);
                    if (out.size() >= 200) break;
                }
            }
            return out;
        }

        return Collections.emptyList();
    }
}
