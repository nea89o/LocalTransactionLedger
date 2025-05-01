{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      overlays = [];
      pkgs = import nixpkgs {inherit system overlays;};
      deps = [
        pkgs.jdk8
        pkgs.jdk21
        pkgs.nodejs_22
        pkgs.nodePackages.pnpm

      ];
    in
      with pkgs; {
        devShells.default = mkShell {
          buildInputs = deps;
        };
        formatter = alejandra;
      });
}