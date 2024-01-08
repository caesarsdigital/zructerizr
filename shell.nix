with import <nixpkgs> { };
let
  nixpkgsVersion = "nixos-23.11"; 

  stablepkgs = import (builtins.fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/${nixpkgsVersion}.tar.gz";
  }) {};
in

  mkShell {
    buildInputs = [
      stablepkgs.coursier
      stablepkgs.jdk17_headless  # or another version of JDK
    ];

    shellHook = ''
      export JAVA_HOME=${stablepkgs.jdk17_headless}
      export PATH="$PATH:$HOME/.local/share/coursier/bin"
      cs install sbt metals
    '';
  }


