
SUMMARY = "Concierge, system manager for FoyerOS"
LICENSE = "MIT"

inherit systemd
inherit cargo_bin
inherit pkgconfig
inherit useradd

SRC_URI = "git://git@github.com/FoyerOS/foyer-concierge.git;protocol=ssh;branch=main;"
SRC_URI += "file://concierge.toml"
SRCREV = "${AUTOREV}"
LIC_FILES_CHKSUM = "file://LICENSE-MIT;md5=d0446e2f7f0432333446c348800d24fa"

# pam-client -> pam-sys links libpam and generates its bindings with bindgen at
# build time, so clang has to be pointed at the target sysroot rather than the
# build host's /usr/include.
DEPENDS += "libpam clang-native"

# cargo_bin (meta-rust-bin) builds with debug info but, unlike oe-core's
# rust-common, sets no --remap-path-prefix, so WORKDIR paths (crate sources
# under CARGO_HOME) end up in the binaries and trip the buildpaths QA check.
EXTRA_RUSTFLAGS += "--remap-path-prefix=${WORKDIR}=${TARGET_DBGSRC_DIR}"

export LIBCLANG_PATH = "${STAGING_LIBDIR_NATIVE}"
BINDGEN_EXTRA_CLANG_ARGS = "${HOST_CC_ARCH}${TOOLCHAIN_OPTIONS} --target=${TARGET_SYS}"
export BINDGEN_EXTRA_CLANG_ARGS

do_compile[network] = "1"

# The daemon rejects logins from users outside this group, and errors out if the
# group is missing entirely. Membership is left to the operator.
USERADD_PACKAGES = "${PN}"
GROUPADD_PARAM:${PN} = "-r foyer-admin"

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
