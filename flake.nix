{
  description = "Myophoria - development environment";

  inputs = {
    # Rolling branch so a current JDK (25 LTS) is available. Pin a stable
    # release branch later if you want fewer surprises. Commit flake.lock.
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = {
    self,
    nixpkgs,
  }: let
    systems = [
      "x86_64-linux"
    ];
    forAllSystems = f:
      nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
  in {
    devShells = forAllSystems (pkgs: let
      # Java 25 is the current LTS. If this attribute is missing on the
      # branch you pinned, use pkgs.jdk21 and set the Maven `release`
      # to 21 so both machines compile for the same version.
      jdk = pkgs.jdk25;
    in {
      default = pkgs.mkShell {
        packages = [
          # Backend
          jdk
          pkgs.maven # bootstraps the Maven Wrapper (mvnw); day to day use ./mvnw

          # Front end (Svelte + Vite + TypeScript + Tailwind)
          pkgs.nodejs

          # Data
          pkgs.sqlite # sqlite3 CLI for inspecting the database

          # Tooling
          pkgs.git
          pkgs.gh
          pkgs.curl
          pkgs.httpie
          pkgs.jq
        ];

        # Maven and IDEs read JAVA_HOME; .home is the correct path
        # inside the OpenJDK derivation.
        JAVA_HOME = "${jdk.home}";

        shellHook = ''
          echo "Myophoria dev shell: $(java -version 2>&1 | head -n 1) | node $(node --version)"
        '';
      };
    });

    formatter = forAllSystems (pkgs: pkgs.alejandra);
  };
}