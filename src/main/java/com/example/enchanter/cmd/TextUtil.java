package com.example.enchanter.cmd;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {
    private TextUtil(){}

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** Build a Component from legacy (with § codes) and force non-italic. */
    public static Component legacyNoItalics(String legacy){
        if (legacy == null) legacy = "";
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    /** Serialize a Component back to legacy (§ codes). */
    public static String toLegacy(Component c){
        return LEGACY.serialize(c);
    }
}
