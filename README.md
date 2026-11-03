# HeadlessMc - Pre-patched-LWJGL

[HeadlessMc](https://github.com/headlesshq/headlessmc) allows you to launch the Minecraft client in headless mode, without rendering anything.
In order to do so, it patches the LWJGL library Minecraft uses for rendering, mouse/keyboard input, etc.
The original approach to patching was fairly simple: we make every method of LWJGL return a dummy value.
However, some methods need special handling, like all methods returning Buffers of a size dependent on a method parameter,
or some methods signaling success/failure via a returned integer.
Back when we developed it, we would launch the game, see if it crashed, check which method needed special handling,
patch it and launch again, until the game was able to launch into a SinglePlayer world.
Now this is great for lazy developers, but far from a comprehensive patch.
Any mod or Minecraft unexpectedly calling a method that needed special handling would crash,
and new versions of Minecraft often needed additional patches.

This project attempts to provide comprehensive patches for the latest versions of LWJGL 2 and 3.
We went through all non-trivial methods and determined the best way to patch them.
This project was machine assisted, as this is probably the type of painstaking and verifiable domain where AI can shine.

## Java agent

`headlessmc-lwjgl-agent-<version>.jar`, attached to every release, makes the LWJGL of an application headless without
HeadlessMc. It bundles the patched LWJGL 2 and 3 jars and the input API:

```
java -javaagent:headlessmc-lwjgl-agent.jar -cp <classpath with the original LWJGL jars> net.minecraft.client.main.Main ...
```

Add the `-javaagent` argument to the JVM arguments of any launcher. The agent

- detects the LWJGL flavour from the LWJGL jars on the classpath, or takes it as argument, `-javaagent:headlessmc-lwjgl-agent.jar=lwjgl3`
  (or `=lwjgl2`, or `-Dhmc.lwjgl.agent.flavor=...`),
- extracts the patched jars to `hmc.lwjgl.agent.directory` (default: `headlessmc-lwjgl-agent` in the temporary directory),
- replaces every LWJGL class loaded from the original jars with the patched class and appends the patched jars and the
  API to the system class loader,
- rewrites `java.class.path`, and `legacyClassPath`/`legacyClassPath.file` of the BootstrapLauncher of Forge and NeoForge,
  so that launchers building their own class loaders or module layers from them load the patched jars,
- sets `joml.nounsafe=true`, like HeadlessMc.

`-Dhmc.lwjgl.agent.debug=true` logs every replaced jar. Attaching the agent to a running JVM is not supported, LWJGL might
be loaded already. Other than HeadlessMc, the agent does not replace assets or handle the macOS menu bar of Minecraft 26.x.
