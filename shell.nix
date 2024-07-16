with import <nixpkgs> { };
let

  nixpkgsVersion = "24.05";
  stablepkgs = import (builtins.fetchTarball {
    url = "https://github.com/NixOS/nixpkgs/archive/${nixpkgsVersion}.tar.gz";
  }) {};
  # We need unstable to track the frequent updates to metals that tend to
  # break compatibility with prior bloop versions
  unstable = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/nixos-unstable.tar.gz") {};
in

  mkShell {
    buildInputs = [
      stablepkgs.coursier
      stablepkgs.jdk
      stablepkgs.graphviz # just for visualizing PlantUML
      unstable.bloop 
    ];

    shellHook = ''
      export JAVA_HOME=${jdk}
      export PATH="$PATH:$HOME/.local/share/coursier/bin"
      cs install sbt metals
      sbt bloopInstall
    '';
  }


