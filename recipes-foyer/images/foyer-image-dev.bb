require foyer-image.bb


IMAGE_FEATURES += "dbg-pkgs dev-pkgs ssh-server-openssh tools-debug empty-root-password allow-empty-password allow-root-login post-install-logging"

# dbg-pkgs and dev-pkgs (debug symbols, headers, static libs, the full gcc
# toolchain) take this rootfs to roughly 4G against production's ~856M, so the
# dev disk image needs correspondingly larger root slots. Only the .wic
# geometry changes; RAUC slot definitions carry no sizes.
FOYER_ROOT_SLOT_SIZE = "6G"
