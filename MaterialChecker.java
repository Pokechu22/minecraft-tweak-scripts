// Code to get vanilla and bukkit IDs using burger and a bukkit jar.
// Originally posted on https://www.reddit.com/r/admincraft/comments/5kew9g/is_there_a_translation_guide_somewhere_between/dbng45s/
// Must be run with a craftbukkit or spigot jar on the classpath
import com.google.gson.Gson;
import java.io.FileReader;
import java.util.Map;
import org.bukkit.Material;

public class MaterialChecker {
    private static Burger burger;
    public static void main(String[] args) throws Exception {
        try (FileReader reader = new FileReader(args[0])) {
            // Burger files contain an array of verisons; we want the first version
            burger = new Gson().fromJson(reader, Burger[].class)[0];
        }
        System.out.println("Using burger data for " + burger.source.file);
        // OK, now that we've got the item table; look through materials.
        System.out.printf("|%s|%s|%s|%s|%n", "Bukkit name", "ID", "Vanilla name", "Display name");
        System.out.println("|-|-|-|-|");
        for (Material material : Material.values()) {
            String crapName = material.name();
            int id = material.getId();
            String text_id = null, display_name = null;
            for (ItemOrBlock item : burger.items.item.values()) {
                if (item.numeric_id == id) {
                    text_id = item.text_id;
                    display_name = item.display_name;
                    break;
                }
            }
            for (ItemOrBlock block : burger.blocks.block.values()) {
                if (block.numeric_id == id) {
                    text_id = block.text_id;
                    display_name = block.display_name;
                    break;
                }
            }
            System.out.printf("|`%s`|%s|`%s`|%s|%n", crapName, id, "minecraft:" + text_id, display_name);
        }
    }
    
    private static class Burger {
        public Items items;
        public Blocks blocks;
        public Source source;
    }
    private static class Items {
        public Map<String, ItemOrBlock> item;
    }
    private static class Blocks {
        public Map<String, ItemOrBlock> block;
    }
    private static class ItemOrBlock {
        public String text_id;
        public int numeric_id;
        public String display_name;
    }
    private static class Source {
        public String file;
    }
}
