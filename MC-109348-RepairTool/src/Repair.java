import java.io.*;
import java.util.*;
import com.mojang.nbt.*;
import net.minecraft.world.level.chunk.storage.RegionFile;

public class Repair {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java Repair <regions>");
			return;
		}

		for (String arg : args) {
			File file = new File(arg);
			System.out.println("Processing " + file);
			process(file);
		}
	}

	private static void process(File file) throws Exception {
		RegionFile region = new RegionFile(file);
		for (int chunkX = 0; chunkX <= 31; chunkX++) {
			for (int chunkZ = 0; chunkZ <= 31; chunkZ++) {
				DataInputStream dis = region.getChunkDataInputStream(chunkX, chunkZ);
				if (dis == null) {
					// No chunk data at that location - this is normal
					continue;
				}
				CompoundTag chunk = NbtIo.read(dis);
				CompoundTag level = chunk.getCompound("Level");
				@SuppressWarnings("unchecked")
				ListTag<CompoundTag> tileEntities = (ListTag<CompoundTag>)level.getList("TileEntities");
				Map<Position, CompoundTag> teByPosition = new HashMap<>();
				ListTag<CompoundTag> keptTEs = new ListTag<>("TileEntities");
				if (tileEntities.size() > 0) {
					System.out.println(" --- " + chunkX + ", " + chunkZ + ": " + tileEntities.size() + " tile entities --- ");
				}
				boolean modified = false;
				for (int i = 0; i < tileEntities.size(); i++) {
					CompoundTag te = tileEntities.get(i);
					int x = te.getInt("x");
					int y = te.getInt("y");
					int z = te.getInt("z");
					Position p = new Position(x, y, z);
					if (teByPosition.containsKey(p)) {
						System.out.println("Duplicate TE at " + x + " " + y + " " + z + "!");
						System.out.println("1:");
						teByPosition.get(p).print(System.out);
						System.out.println("2:");
						te.print(System.out);
						modified = true;
					} else {
						teByPosition.put(p, te);
						keptTEs.add(te);
					}
				}
				
				if (modified) {
					System.out.println("Rewriting section!");
					level.put("TileEntities", keptTEs);
					try (DataOutputStream stream = region.getChunkDataOutputStream(chunkX, chunkZ)) {
						NbtIo.write(chunk, stream);
					}
				}
			}
		}
	}

	/** A position in the chunk */
	private static class Position {
		public Position(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			result = prime * result + z;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Position other = (Position) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			if (z != other.z)
				return false;
			return true;
		}
		public int x;
		public int y;
		public int z;
	}
}