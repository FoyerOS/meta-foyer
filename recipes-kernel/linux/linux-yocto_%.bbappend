# kernel-yocto merges every .cfg found in SRC_URI at do_configure, so this is
# enough to fold these fragments into whichever KMACHINE/KBRANCH combination
# MACHINEOVERRIDES selected — see foyer-x86-64.conf and foyer-arm64.conf's
# comments on why those pick up the qemux86-64/qemuarm64 kernel caches
# without their own bbappend.
#
# The qemux86-64/qemuarm64 kernel caches are virtio-centric — fine for QEMU,
# not for bare metal or another hypervisor's guest drivers. These fragments
# add the storage, network and platform support real hardware and other
# clouds need.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://foyer-storage.cfg file://foyer-btrfs.cfg file://foyer-net.cfg file://foyer-platform.cfg"
SRC_URI:append:foyer-x86-64 = " file://foyer-net-x86.cfg file://foyer-platform-x86.cfg"
