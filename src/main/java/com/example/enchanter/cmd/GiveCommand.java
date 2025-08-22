package com.example.enchanter.cmd;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp() && !sender.hasPermission("enchanter.give")) {
            Msg.tell(sender, Msg.fill("You must be OP or have enchanter.give."));
            return true;
        }
        if (args.length < 2) {
            Msg.usage(sender, "/" + label + " <player> <material> [amount] [enchant[:lvl] ...] [name...] [&color]");
            return true;
        }

        Player target = resolvePlayer(sender, args[0]);
        if (target == null) { Msg.tell(sender, "Player not found: " + Msg.fill(args[0])); return true; }

        Material mat = matchMaterial(args[1]);
        if (mat == null) { Msg.tell(sender, "Unknown material: " + Msg.fill(args[1])); return true; }

        // amount is OPTIONAL (default 1)
        int amount = 1;
        int i = 2;
        if (args.length >= 3 && isInt(args[2])) { try { amount = Math.max(1, Integer.parseInt(args[2])); } catch(Exception ignored){} i = 3; }

        // parse enchants until a token is clearly not an enchant (but allow partials for tab-complete logic)
        List<EnchantSpec> enchants = new ArrayList<>();
        for (; i < args.length; i++){
            EnchantSpec spec = parseEnchant(args[i]);
            if (spec == null) break;
            enchants.add(spec);
        }

        // optional name + color
        String colorToken = null;
        if (i < args.length && ColorUtil.isColorToken(args[args.length - 1])) {
            colorToken = args[args.length - 1];
            args = Arrays.copyOf(args, args.length - 1);
        }
        String customName = (i < args.length) ? ColorUtil.normalizeSpaces(String.join(" ", Arrays.copyOfRange(args, i, args.length))) : null;

        giveItems(target, mat, amount, enchants, customName, colorToken);

        Msg.broadcastGive(amount, mat.name().toLowerCase(java.util.Locale.ROOT), target.getName());
        return true;
    }

    // ------- helpers -------
    private static boolean isInt(String s){ if (s==null||s.isEmpty()) return false; for (int i=0;i<s.length();i++) if (!Character.isDigit(s.charAt(i))) return false; return true; }

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
        if (m == null && t.startsWith("MINECRAFT:")) m = Material.matchMaterial(t.substring("MINECRAFT:".length()));
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
        int max = mat.getMaxStackSize(), remaining = total;
        while (remaining > 0){
            int take = Math.min(max, remaining);
            ItemStack stack = new ItemStack(mat, take);
            for (EnchantSpec es : enchants) stack.addUnsafeEnchantment(es.enchant(), es.level());
            if (name != null || color != null){
                var meta = stack.getItemMeta();
                String baseName = (name != null) ? name : pretty(mat);
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

    // ------- tab complete (players, materials, amount, enchants, colors) -------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof ConsoleCommandSender) && !sender.isOp() && !sender.hasPermission("enchanter.give")) return Collections.emptyList();

        // arg1: player
        if (args.length == 1) {
            String pfx = args[0].toLowerCase(java.util.Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                String n = p.getName();
                if (n.toLowerCase(java.util.Locale.ROOT).startsWith(pfx)) names.add(n);
            }
            if ("me".startsWith(pfx)) names.add(0, "me");
            Collections.sort(names); return names;
        }

        // arg2: material
        if (args.length == 2) {
            String pfx = args[1].toLowerCase(java.util.Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Material m : Material.values()) {
                String n = m.name().toLowerCase(java.util.Locale.ROOT);
                if (n.startsWith(pfx)) { out.add(n); if (out.size() >= 200) break; }
            }
            return out;
        }

        // arg3: amount (suggest common amounts even if not yet digits)
        if (args.length == 3) return Arrays.asList("1","8","16","32","64");

        // From here on: enchants / name / color
        String last = args[args.length - 1];

        // If last looks like a color token, suggest colors
        if (last.startsWith("&") || last.startsWith("#")) {
            return Arrays.asList("&0","&1","&2","&3","&4","&5","&6","&7","&8","&9",
                    "&a","&b","&c","&d","&e","&f","&k","&l","&m","&n","&o","&r",
                    "#FFFFFF","#FFAA00","#00FFFF","#FF0000","#00FF00","#AAAAAA");
        }

        // Determine base index (2 or 3) depending on whether amount was provided
        int base = 2;
        if (args.length >= 3 && isInt(args[2])) base = 3;

        // Are all tokens BEFORE 'last' valid enchants? If yes, we're still in the enchant zone → suggest by prefix.
        boolean prevAllEnchants = true;
        for (int i = base; i < args.length - 1; i++) {
            if (parseEnchant(args[i]) == null) { prevAllEnchants = false; break; }
        }

        if (prevAllEnchants) {
            String pfx = last.toLowerCase(java.util.Locale.ROOT);
            // build list of keys + aliases; offer ":1" unless user already typed a colon
            Set<String> keys = new TreeSet<>();
            for (Enchantment e : Enchantment.values()) if (e != null && e.getKey() != null) keys.add(e.getKey().getKey());
            keys.addAll(aliasMap.values());
            List<String> out = new ArrayList<>();
            for (String k : keys) {
                String s = k + (pfx.contains(":") ? "" : ":1");
                if (s.startsWith(pfx)) { out.add(s); if (out.size() >= 200) break; }
            }
            return out;
        }

        // Otherwise we're in name/color area → no suggestions
        return Collections.emptyList();
    }
}

