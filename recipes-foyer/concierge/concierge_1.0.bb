
SUMMARY = "Concierge, system manager for FoyerOS"
LICENSE = "MIT"

inherit systemd
inherit cargo_bin
inherit pkgconfig

SRC_URI = "git://git@github.com/FoyerOS/foyer-concierge.git;protocol=ssh;branch=main;"
SRC_URI += "file://concierge.toml"
SRCREV = "${AUTOREV}"
LIC_FILES_CHKSUM = "file://LICENSE-MIT;md5=d0446e2f7f0432333446c348800d24fa"

# pam-client -> pam-sys links libpam and generates its bindings with bindgen at
# build time, so clang has to be pointed at the target sysroot rather than the
# build host's /usr/include. nodejs-native builds the WebGUI (see do_compile:prepend
# below) before cargo embeds it via the `webgui` feature.
DEPENDS += "libpam clang-native nodejs-native"

# cargo_bin (meta-rust-bin) builds with debug info but, unlike oe-core's
# rust-common, sets no --remap-path-prefix, so WORKDIR paths (crate sources
# under CARGO_HOME) end up in the binaries and trip the buildpaths QA check.
EXTRA_RUSTFLAGS += "--remap-path-prefix=${WORKDIR}=${TARGET_DBGSRC_DIR}"

# --remap-path-prefix above only reaches rustc-compiled sources. ring (pulled
# in transitively via rustls) compiles its C crypto primitives through the
# `cc` crate instead, and cargo_bin_do_compile forces HOST_CFLAGS/HOST_CXXFLAGS
# empty (RUST_BUILD and RUST_TARGET are both the bare triple
# x86_64-unknown-linux-gnu, so cc-rs treats this cross build as a "host" one
# and only honors HOST_CFLAGS), so the C compiler embeds the raw WORKDIR path
# into ring's debug info with nothing to remap it. Only concierge-dbg (debug
# symbols, not installed by default) carries the leaked path, so skip the
# check there rather than fighting cc-rs's host/target flag precedence.
INSANE_SKIP:${PN}-dbg += "buildpaths"

export LIBCLANG_PATH = "${STAGING_LIBDIR_NATIVE}"
BINDGEN_EXTRA_CLANG_ARGS = "${HOST_CC_ARCH}${TOOLCHAIN_OPTIONS} --target=${TARGET_SYS}"
export BINDGEN_EXTRA_CLANG_ARGS

do_compile[network] = "1"

# webgui/dist is gitignored upstream (not reproducible/hermetic to commit a
# build artifact), so it has to be built here before cargo_bin_do_compile
# runs `cargo build --features ${CARGO_FEATURES}` and embeds it via
# rust-embed. Runs inside do_compile, so it shares the network access
# already granted above for cargo's own crates.io fetches.
export npm_config_cache = "${WORKDIR}/npm-cache"
do_compile:prepend() {
    ( cd ${S}/webgui && npm ci && npm run build )
}

CARGO_FEATURES = "webgui"

# The daemon rejects logins from users outside this group, and errors out if
# the group is missing entirely. The group (and the admin account that is a
# member of it) is created by foyer-base-users, not here — two recipes
# creating the same system group is an ordering hazard.
RDEPENDS:${PN} += "foyer-base-users"

# The unit and PAM service ship in the upstream repo, so they stay in step with
# the binary; this layer only packages them.
do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/systemd/foyer-concierge.service ${D}${systemd_system_unitdir}/foyer-concierge.service

    install -d ${D}${sysconfdir}/pam.d
    install -m 0644 ${S}/systemd/pam.d/foyer-concierge ${D}${sysconfdir}/pam.d/foyer-concierge

    install -d ${D}${sysconfdir}/foyer
    install -m 0644 ${UNPACKDIR}/concierge.toml ${D}${sysconfdir}/foyer/concierge.toml
}

SYSTEMD_SERVICE:${PN} = "foyer-concierge.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# /etc/pam.d/common-auth and common-account, which our PAM service includes.
RDEPENDS:${PN} += "libpam-runtime"

CONFFILES:${PN} = "${sysconfdir}/foyer/concierge.toml"
