# Changelog

## [0.1.1](https://github.com/headlesshq/headlessmc-lwjgl/compare/0.1.0...0.1.1) (2026-10-30)


### Bug Fixes

* **modules:** make the patched LWJGL work on module layers ([#2](https://github.com/headlesshq/headlessmc-lwjgl/issues/2)) ([5b8dd30](https://github.com/headlesshq/headlessmc-lwjgl/commit/5b8dd305a08115d035c81b2439469b9678ec7047))

## 0.1.0 (2026-10-29)


### Features

* **glfw:** inject input through the API into GLFW, add the LWJGL probe tool ([c71c31c](https://github.com/headlesshq/headlessmc-lwjgl/commit/c71c31c7affecea18b6780723fc04b6e42b5d171))
* **glfw:** track GLFW window hints, refine generated GL values ([a5e0b8e](https://github.com/headlesshq/headlessmc-lwjgl/commit/a5e0b8eb8ff3dc6ce79262b313ad95714d13e0d4))
* **lwjgl2:** add the LWJGL 2 project, share GL values and state ([fec0424](https://github.com/headlesshq/headlessmc-lwjgl/commit/fec0424c13759cb5c51aa34e0ce52075f7fd731a))
* **lwjgl2:** patch LWJGL 2.9.4 for Minecraft 1.12.2 and older ([9fa3616](https://github.com/headlesshq/headlessmc-lwjgl/commit/9fa36164b88f5d735dea77cd80930b9dd6584562))
* **lwjgl3:** patch every LWJGL 3 method headlessly, phase 1 ([f9e48f2](https://github.com/headlesshq/headlessmc-lwjgl/commit/f9e48f2478971e1c2aa631bff83dfcba91607f54))
* **lwjgl3:** re-add the public API of older LWJGL versions and gate it with japicmp ([184bffe](https://github.com/headlesshq/headlessmc-lwjgl/commit/184bffe5c22665749517dd8316b65e77505ce290))
* **lwjgl3:** support OpenAL extensions, STB image writing and headless detection ([553ae72](https://github.com/headlesshq/headlessmc-lwjgl/commit/553ae7226475ad2d6aa44909621bc4ffcf670ee6))
* **memory:** give structs real memory ([c5d7a0e](https://github.com/headlesshq/headlessmc-lwjgl/commit/c5d7a0e4363b0754dac3ca25ce430741aca808d1))
* **openal:** track OpenAL sources, buffers and the listener ([f926cb7](https://github.com/headlesshq/headlessmc-lwjgl/commit/f926cb7e48b702e7025ac9a8f3fe763fc429cda4))
* **opengl:** answer indexed GL queries and list GLFW monitors ([646d259](https://github.com/headlesshq/headlessmc-lwjgl/commit/646d2592fbbb21c6ba85d6cc514e38e96b6ae0a3))
* **opengl:** answer shader and program queries per parameter ([def3d80](https://github.com/headlesshq/headlessmc-lwjgl/commit/def3d8016a95f4dcfe27c3b21fdf01a4042080db))
* **opengl:** generate the glGet defaults from the Mesa llvmpipe recording ([d8d141d](https://github.com/headlesshq/headlessmc-lwjgl/commit/d8d141dc56acfe5b6a5741ddb66a70401776c778))
* **opengl:** track OpenGL state that applications query back ([239e356](https://github.com/headlesshq/headlessmc-lwjgl/commit/239e3567a5b07d2563c81960c58d82a1ff9a0e22))
* **opengl:** track texture image sizes for glGetTexLevelParameter* ([f2cc4e1](https://github.com/headlesshq/headlessmc-lwjgl/commit/f2cc4e13a3a3b822653c8eefe73ce32ee43b0383))
* **opengl:** track texture parameters, framebuffer attachments and renderbuffers ([478d497](https://github.com/headlesshq/headlessmc-lwjgl/commit/478d49712d301034898e87af931fef7805d2cfca))
* **sdl:** add the SDL probe suite and fix what it found ([92eac9c](https://github.com/headlesshq/headlessmc-lwjgl/commit/92eac9cf7a51f3abcac3bcdeb57a9957e754ca80))
* **sdl:** inject input into SDL applications ([90fa084](https://github.com/headlesshq/headlessmc-lwjgl/commit/90fa084cea79ef89b826f3e88f2d971d5e311764))


### Bug Fixes

* **lwjgl3:** fix what Minecraft 1.21.1 needs to reach a world ([f2576da](https://github.com/headlesshq/headlessmc-lwjgl/commit/f2576dab7f0f4c60adca42aa070e454538a434a3))
* **lwjgl3:** run Minecraft 26.3 without Vulkan, with SDL GL attributes and working PointerBuffers ([f85aea6](https://github.com/headlesshq/headlessmc-lwjgl/commit/f85aea6eb490b68965e7f538dc77dccbdc1ff7d3))
* **lwjgl3:** support the Forge and NeoForge early loading screens ([cceb5c6](https://github.com/headlesshq/headlessmc-lwjgl/commit/cceb5c638a2fd8443c635b0583a67abb2abb32bd))
* **memory:** make callbacks, MemoryUtil strings and small allocations work ([d448e9b](https://github.com/headlesshq/headlessmc-lwjgl/commit/d448e9b7aac193d6d01cb089e4904886707a3a0f))
* **opengl:** fix values found by comparing with the Mesa recording ([c1bbe75](https://github.com/headlesshq/headlessmc-lwjgl/commit/c1bbe75992af8e248f3e30178ef7133169db75dc))
* **patcher:** keep bridge methods and give LWJGL 2 a real drawable ([c3fd8f6](https://github.com/headlesshq/headlessmc-lwjgl/commit/c3fd8f673a464f115078ae7c96acf9e1bd7fc276))
* **patcher:** treat every created GL and AL object as existing ([5b4d297](https://github.com/headlesshq/headlessmc-lwjgl/commit/5b4d297cc89d1f86aa989e5b510b735dc13567c0))
* **probe:** skip unavailable functions, fix X11 under Xvfb, load recorded GL values ([3eb3033](https://github.com/headlesshq/headlessmc-lwjgl/commit/3eb3033499cec664629c8a288873cb8a5a5cd4b2))
