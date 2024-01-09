with import <nixpkgs> { };
let
  nixpkgsVersion = "nixos-23.11"; 

  stablepkgs = import (builtins.fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/${nixpkgsVersion}.tar.gz";
  }) {};
  jdk = stablepkgs.jdk17;
in

  mkShell {
    buildInputs = [
      stablepkgs.coursier
      jdk
      graphviz # just for visualizing PlantUML
    ];

    shellHook = ''
      export JAVA_HOME=${jdk}
      export PATH="$PATH:$HOME/.local/share/coursier/bin"
      cs install sbt metals
      sbt bloopInstall
    '';
  }


