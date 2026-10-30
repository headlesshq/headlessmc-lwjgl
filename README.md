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
