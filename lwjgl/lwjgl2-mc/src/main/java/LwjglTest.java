import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;

public class LwjglTest {
	public static void main(String[] args) throws Exception {
		Display.setDisplayMode(new DisplayMode(480, 360));

		Display.setResizable(true);
		Display.setTitle("LWJGL2 test (press escape to close)");

		Display.create((new PixelFormat()).withDepthBits(24));

		mainLoop: while (true) {
			while (Keyboard.next()) {
				char c0 = Keyboard.getEventCharacter();

				if (Keyboard.getEventKey() == 0 && c0 >= 32 || Keyboard.getEventKeyState()) {
					keyTyped(c0, Keyboard.getEventKey());
					if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
						break mainLoop;
					}
				}
			}
			Display.update();
			Thread.yield();
		}
		System.out.println("Exiting...");
	}

	private static void keyTyped(char c, int i) {
		System.out.printf("%s (%d) was typed%n", c, i);
	}
}