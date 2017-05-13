import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.GL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;

public class LwjglTest {
	private static GLFWErrorCallback errorCallback;

	private static GLFWKeyCallback keyCallback;
	private static GLFWCharCallback charCallback;
	private static long window;

	private static volatile boolean remainOpen = true;
	public static void main(String[] args) throws Exception {
		glfwInit();
		glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err)); 
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

		window = glfwCreateWindow(480, 360, "LWJGL3 test (press escape to close)", 0, 0);
		if(window == 0) throw new RuntimeException("Failed to create window");

		glfwMakeContextCurrent(window);
		GL.createCapabilities();
		glfwShowWindow(window);

		glfwSetKeyCallback(window, (keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (key == GLFW_KEY_ESCAPE) {
					remainOpen = false;
				}
			}
		}));

		glfwSetCharCallback(window, (charCallback = new GLFWCharCallback() {
			@Override
			public void invoke(long window, int codepoint) {
				keyTyped((char)codepoint, codepoint);
			}
		}));

		mainLoop: while (remainOpen) {
			glfwPollEvents();
			glfwSwapBuffers(window);
			Thread.yield();
		}
		System.out.println("Exiting...");
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private static void keyTyped(char c, int i) {
		System.out.printf("%s (%d) was typed%n", c, i);
	}
}