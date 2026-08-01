SUMMARY = "Foyer's built-in admin account"
DESCRIPTION = "Creates the foyer-admin group and a non-root admin user, with a \
placeholder password that must be changed at first login. Concierge \
authenticates and authorizes over PAM against this account rather than root; \
see crates/concierge/src/daemon/auth.rs in foyer-concierge for the \
NEW_AUTHTOK_REQD handling this depends on."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# Nothing to compile: this recipe's only content is the postinst script
# useradd.bbclass attaches to ${PN}.
INHIBIT_DEFAULT_DEPS = "1"

inherit useradd

# No compiled content, just the group/user creation below — the package
# carries only the postinst scriptlet useradd.bbclass attaches to it.
PACKAGES = "${PN}"
FILES:${PN} = ""
ALLOW_EMPTY:${PN} = "1"
USERADD_PACKAGES = "${PN}"

# Moved here from concierge_1.0.bb: two recipes creating the same system group
# is an ordering hazard, and the admin user below needs the group to already
# exist at the point useradd.bbclass creates it.
GROUPADD_PARAM:${PN} = "-r foyer-admin"

# Password is the SHA-512 crypt (openssl passwd -6 -salt foyeradminsalt
# 'foyer-admin') of the literal string "foyer-admin" — a placeholder, not a
# secret: the shadow last-changed field is forced to 0 by
# foyer-image.bb's ROOTFS_POSTPROCESS_COMMAND, which makes pam_unix require a
# new password before this account can do anything else. Anyone with local
# rootfs read access can see this hash anyway, so there is nothing to protect
# by hiding it.
#
# The dollar signs are backslash-escaped: useradd.bbclass's generated preinst
# script assigns USERADD_PARAM="${USERADD_PARAM}" — a *double-quoted* shell
# string — so an unescaped $6/$4 there is read as shell positional
# parameters (both unset, both expanding to nothing) rather than literal
# text, silently truncating the hash. \$ survives that pass as a literal $.
FOYER_ADMIN_PLACEHOLDER_HASH = "\$6\$foyeradminsalt\$4BYDRKv6h2S2oaIh/wlY4CUZTrqB.gFdFGHIrLBAdRsRU8oDaEIZSM7/ZCqUZs56tYMYCFd0xYj5O.A4KGncJ1"
USERADD_PARAM:${PN} = "-m -d /home/admin -s /bin/sh -G foyer-admin -p '${FOYER_ADMIN_PLACEHOLDER_HASH}' admin"
