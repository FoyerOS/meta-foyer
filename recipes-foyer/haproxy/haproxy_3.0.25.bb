SUMMARY = "Fast, reliable, high-performance TCP/HTTP load balancer and reverse proxy"
HOMEPAGE = "https://www.haproxy.org/"
SECTION = "net"

LICENSE = "GPL-2.0-with-OpenSSL-exception"
LIC_FILES_CHKSUM = "file://LICENSE;md5=2d862e836f92129cdc0ecccc54eed5e0"

SRC_URI = "https://www.haproxy.org/download/3.0/src/haproxy-${PV}.tar.gz \
           file://haproxy.cfg \
           file://haproxy.service \
          "
SRC_URI[sha256sum] = "0e4fbd90826368297ce4d5374596dd1eff58f3ec00c27312a86e455e4fe1884f"

inherit systemd useradd

# TLS termination for concierge's `tls enable`/`disable`/`set-ca` (see
# foyer-concierge's daemon/services/tls.rs, which rewrites haproxy.cfg to
# add a `bind *:443 ssl crt ...` frontend). No PCRE/Lua: nothing here needs
# regex-based ACLs or scripting.
DEPENDS = "openssl"

# In 3.0.x the Makefile's CPU=/ARCH= variables are deprecated no-ops; only
# TARGET= still matters. CC/CFLAGS/LDFLAGS must be passed as explicit make
# arguments (not just inherited from the environment) because they're only
# ever '='-assigned inside the Makefile, and a command-line argument is the
# only thing that beats that.
# src/haproxy.o additionally bakes VERBOSE_CFLAGS (== CFLAGS plus recipe-sysroot
# -I paths) into the binary via -DBUILD_CFLAGS, purely for `haproxy -vv`
# diagnostics. -fdebug-prefix-map doesn't touch it since it's a plain string
# literal, not debug info, so it trips the buildpaths QA check; blank it since
# nothing depends on that diagnostic string being populated.
#
# USE_OPENSSL=1 needs no explicit SSL_INC/SSL_LIB: CC already carries
# --sysroot=<recipe-sysroot> (see the BUILD_CC note below), and that's where
# DEPENDS="openssl" stages headers/libs, so the default search paths resolve.
EXTRA_OEMAKE = " \
    TARGET=linux-glibc \
    USE_OPENSSL=1 \
    'CC=${CC}' \
    'CFLAGS=${CFLAGS}' \
    'LDFLAGS=${LDFLAGS}' \
    'VERBOSE_CFLAGS=' \
    "

# The same src/haproxy.o rule also embeds -DBUILD_CC='"$(CC)"' verbatim, and
# our cross CC string itself contains --sysroot=<recipe-sysroot>, which is
# under TMPDIR. Unlike BUILD_CFLAGS, this one can't be blanked via a make
# variable without breaking the actual compile (CC must keep --sysroot to
# build at all) — it would need a Makefile patch to fix upstream. It's a
# diagnostic-only string (visible via `haproxy -vv`), so skip the check
# rather than patch around it; same pattern as netdata_1.47.5.bb in meta-oe.
INSANE_SKIP:${PN} += "buildpaths"

do_compile() {
    oe_runmake ${EXTRA_OEMAKE}
}

do_install() {
    oe_runmake ${EXTRA_OEMAKE} \
        DESTDIR="${D}" PREFIX="${prefix}" SBINDIR="${sbindir}" \
        MANDIR="${mandir}" DOCDIR="${docdir}/haproxy" \
        install

    install -d ${D}${sysconfdir}/haproxy
    install -m 0644 ${UNPACKDIR}/haproxy.cfg ${D}${sysconfdir}/haproxy/haproxy.cfg

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/haproxy.service ${D}${systemd_system_unitdir}/haproxy.service

    install -d ${D}${sysconfdir}/tmpfiles.d
    echo "d /run/haproxy 0755 haproxy haproxy -" > ${D}${sysconfdir}/tmpfiles.d/haproxy.conf
}

FILES:${PN} += "${sysconfdir}/tmpfiles.d/haproxy.conf"

SYSTEMD_SERVICE:${PN} = "haproxy.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# haproxy binds as root then drops to this user/group via the global
# user/group directives in haproxy.cfg, same least-privilege pattern as
# concierge's foyer-admin group.
USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "--system --no-create-home --shell ${base_sbindir}/nologin --user-group haproxy"

# Concierge regenerates and reloads this file at runtime (see `concierge
# tls`); CONFFILES marks it operator/tool-owned so a package upgrade never
# clobbers a live config, same semantics as concierge.toml in
# concierge_1.0.bb. /etc persists across RAUC A/B updates via overlayfs-etc.
CONFFILES:${PN} = "${sysconfdir}/haproxy/haproxy.cfg"
