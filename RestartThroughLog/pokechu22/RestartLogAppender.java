package pokechu22;

import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.io.Serializable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name="Restarter", category="Core", elementType="appender", printObject=true)
public class RestartLogAppender extends AbstractAppender {
	private MinecraftServer server = null;
	private final Method shutdownMethod;
	private final File restartScript;
	private final int shutdownDelay;
	/**
	 * Time the first shutdown was attempted at, or 0
	 */
	private long shutdownAttemptedAt = 0;
	/**
	 * Has a restart already been queued?  If so we don't want to do another one.
	 */
	private boolean queuedRestart = false;

	public RestartLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Method shutdownMethod, File restartScript, int shutdownDelay) {
		super(name, filter, layout, ignoreExceptions);
		this.shutdownMethod = shutdownMethod;
		this.restartScript = restartScript;
		this.shutdownDelay = shutdownDelay;
	}

	@Override
	public void append(LogEvent event) {
		// Called when a message matches the filter.
		if (shutdownAttemptedAt == 0) {
			shutdownAttemptedAt = System.currentTimeMillis();
			LOGGER.info("Attempting to shut down server");
			shutdownServer();
		} else {
			if (System.currentTimeMillis() - shutdownAttemptedAt <= shutdownDelay) {
				LOGGER.warn("Server already told to shut down; waiting for it to finish");
			} else {
				LOGGER.error("Ok, enough time has been given - DIE, SERVER, DIE!");
				System.exit(-1);
			}
		}
		queueRestartIfNeeded();
	}

	public void shutdownServer() {
		try {
			shutdownMethod.invoke(getServer());
		} catch (Exception e) {
			LOGGER.error("Failed to shut down server", e);
		}
	}
	
	/**
	 * Finds and gets the current server.
	 */
	public MinecraftServer getServer() {
		if (this.server == null) {
			LOGGER.debug("Finding the server");
			for (Thread thread : Thread.getAllStackTraces().keySet()) {
				Runnable runnable = getThreadTarget(thread);
				LOGGER.trace(thread + ": " + runnable);
				if (runnable instanceof MinecraftServer) {
					this.server = (MinecraftServer)runnable;
					break;
				}
			}
			LOGGER.debug("Found the server: " + this.server);
		}
		return this.server;
	}

	/**
	 * Gets the private "target" Runnable of a Thread.
	 */
	private Runnable getThreadTarget(Thread thread) {
		try {
			Field field = Thread.class.getDeclaredField("target");
			field.setAccessible(true);
			return (Runnable)field.get(thread);
		} catch (Exception e) {
			LOGGER.warn("Failed to get thread target for " + thread, e);
		}
		return null;
	}
	/**
	 * Queues a new instance of the server if one hasn't already been queued.
	 */
	private void queueRestartIfNeeded() {
		if (queuedRestart) {
			return;
		}

		queuedRestart = true;
		// Based off of the logic for spigot's /restart
		Thread shutdownHook = new Thread() {
			@Override
			public void run() {
				try {
					LOGGER.info("Preparing to restart server");
					String os = System.getProperty("os.name").toLowerCase();
					if (os.contains("win")) {
						Runtime.getRuntime().exec("cmd /c start " + restartScript.getPath());
					} else {
						Runtime.getRuntime().exec(new String[] {"sh", restartScript.getPath()});
					}
				} catch (Exception e) {
					LOGGER.error("Failed to restart server", e);
				}
			}
		};

		shutdownHook.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	@PluginFactory
	public static RestartLogAppender createAppender(@PluginAttribute("name") String name, @PluginAttribute("ignoreExceptions") String ignore, @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filters") Filter filter, @PluginAttribute("target") String target, @PluginAttribute("shutdownMethod") String shutdownMethodName, @PluginAttribute("restartScript") String restartScriptPath, @PluginAttribute("shutdownDelay") String shutdownDelayStr) {
		boolean ignoreExceptions = Boolean.parseBoolean(ignore);
		LOGGER.info("Preparing RestartLogAppender");
		if (name == null) {
			LOGGER.error("No name provided for RestartLogAppender");
			return null;
		}
		if (shutdownMethodName == null) {
			LOGGER.error("Must specify the shutdown method");
			return null;
		}
		Method shutdownMethod;
		try {
			shutdownMethod = MinecraftServer.class.getMethod(shutdownMethodName);
		} catch (NoSuchMethodException e) {
			LOGGER.error("There is no method named " + shutdownMethodName + " in " + MinecraftServer.class, e);
			return null;
		}
		if (restartScriptPath == null) {
			LOGGER.error("Must specify the restart script");
			return null;
		}
		File restartScript = new File(restartScriptPath);
		if (!restartScript.exists()) {
			LOGGER.error("Restart script (" + restartScript.getPath() + ") does not exist!");
			return null;
		}
		
		if (layout == null) {
			layout = PatternLayout.createLayout(null, null, null, null, null);
		}
		
		return new RestartLogAppender(name, filter, layout, ignoreExceptions, shutdownMethod, restartScript, Integer.parseInt(shutdownDelayStr));
	}
}
