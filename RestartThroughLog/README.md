# RestartLogAppender

A way to force the server to restart when something appears in the log.

## Concept

This works by extending Minecraft's log4j-based logging, and then using a custom appender that restarts the server when a message passes its filters.

## Usage

First, copy the attached `restartlogappender.zip` into the same directory as your server.  Then create a script named `start.bat` (windows) containing:

```
java -Dlog4j.configurationFile=<log file.xml> -cp restartlogappender.zip;<server.jar> net.minecraft.server.MinecraftServer nogui
```

or `start.sh` (linux) containing:

```
#!/bin/sh
java -Dlog4j.configurationFile=<log file.xml> -cp restartlogappender.zip\;<server.jar> net.minecraft.server.MinecraftServer nogui
```

Then, copy the appropriate log XML file for your setup into the server directory.  Replace `<log file.xml>` in the startup script with the name of the log file you chose (either `log4j2-forge.xml` or `log4j2-bukkit.xml`), and replace `<server.jar>` with the actual name of your server jar.

Edit the XML configuration and find the part of it and change the `RegexFilter` to match what you want to work with (simply replacing "Insert error message here" with the exact message you want to find will work).  See [this article](https://logging.apache.org/log4j/2.x/manual/filters.html) for more information about possible filters.

Then, start the server using the startup script.  You're done!