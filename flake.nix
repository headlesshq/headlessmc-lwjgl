{
  description = "Development shell for headlessmc-lwjgl";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs =
    { nixpkgs, ... }:
    let
      inherit (nixpkgs) lib;

      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
      ];

      forAllSystems = f: lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});

      # check runs testJava8, testJava17 and testJava21 on these toolchains
      jdks = pkgs: {
        "25" = pkgs.temurin-bin-25;
        # Temurin has no JDK 8 for aarch64-darwin
        "8" =
          if pkgs.stdenv.hostPlatform.system == "aarch64-darwin" then pkgs.zulu8 else pkgs.temurin-bin-8;
        "17" = pkgs.temurin-bin-17;
        "21" = pkgs.temurin-bin-21;
      };

      mkJavaShell =
        pkgs: primary:
        let
          available = jdks pkgs;
          jdk = available.${primary};
        in
        pkgs.mkShell (
          {
            packages = [ jdk ];

            JAVA_HOME = jdk.home;
          }
          // lib.optionalAttrs (lib.length (lib.attrNames available) > 1) {
            # Gradle ignores org.gradle.java.installations.paths in GRADLE_OPTS,
            # but its toolchain detection scans $SDKMAN_CANDIDATES_DIR/java/*
            SDKMAN_CANDIDATES_DIR = pkgs.linkFarm "jdks" (
              lib.mapAttrs' (version: j: lib.nameValuePair "java/${version}" j.home) available
            );
          }
        );
    in
    {
      devShells = forAllSystems (pkgs: {
        default = mkJavaShell pkgs "25";
        java8 = mkJavaShell pkgs "8";
        java17 = mkJavaShell pkgs "17";
        java21 = mkJavaShell pkgs "21";
      });

      formatter = forAllSystems (pkgs: pkgs.nixfmt-tree);
    };
}
