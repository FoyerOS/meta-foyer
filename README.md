This README file contains information on the contents of the meta-foyer layer.

Please see the corresponding sections below for details.

Dependencies
============

  URI: https://git.openembedded.org/openembedded-core
  branch: yocto-6.1_M1

  URI: https://git.openembedded.org/meta-openembedded
  branch: wrynose

Patches
=======

Please submit any patches against the meta-foyer layer as a pull request against:

  https://github.com/FoyerOS/meta-foyer

Maintainer: rfleuryleveso <mail@rflv.fr>

Table of Contents
=================

  I. Adding the meta-foyer layer to your build
 II. Disk layout and A/B updates
III. Misc


I. Adding the meta-foyer layer to your build
=================================================

meta-foyer is normally pulled in automatically as part of the foyer-os kas
project (see kas/base.yml in foyer-os). To add it to an existing bitbake
build manually instead, run:

  bitbake-layers add-layer meta-foyer

II. Disk layout and A/B updates
===============================

Foyer ships a single GPT layout, defined in files/wic/foyer.wks.in:

  1 esp           vfat   grub-efi, grub.cfg, grubenv       not updated
  2 rootA         ext4   OS slot A                         A/B
  3 rootB         ext4   OS slot B                         A/B
  4 seedA         ext4   container images for slot A       A/B
  5 seedB         ext4   container images for slot B       A/B
  6 foyer-config  ext4   /etc overlay upper layer          never updated
  7 foyer-data    ext4   all runtime state                 never updated

Everything is addressed by *filesystem label*, never by device node, so the
same image boots from vda under QEMU, sda or nvme0n1 on bare metal, and
mmcblk0 on an SBC. Each partition also carries a matching GPT partition name,
because the kernel's root= understands PARTLABEL= but not LABEL= without an
initramfs, while grub's `search --label` and blkid -L read the filesystem
label. Both must stay in sync when editing the .wks.

At runtime:

  /            rootA or rootB, read-only, chosen by grub
  /efi         the ESP; RAUC edits /efi/EFI/BOOT/grubenv here
  /etc         an overlay whose upper layer is on foyer-config
  /data        foyer-data, with /var/lib/{containers,homeassistant,foyer}
               bind-mounted onto it
  /usr/share/foyer/seed   the seed slot matching the booted root slot

The data partition is shipped small and grown to fill the disk on first boot
by foyer-grow-data.service, so one image suits an SD card and a large SSD.

Signing keys for RAUC
---------------------

No key material is committed to this layer. Generate a development CA once:

  cd foyer-os
  kas shell kas/base.yml -c "../../meta-foyer/scripts/foyer-rauc-dev-ca"

That writes ca.cert.pem and ca.key.pem into $BUILDDIR/foyer-pki. The next
build bakes the certificate in as /usr/lib/rauc/ca.cert.pem, and
foyer-bundle.bb signs with the key. Point FOYER_RAUC_PKI_DIR elsewhere in
conf/local.conf if you keep the keys somewhere else. Use a real PKI, not this
CA, for anything you actually deploy.

For real devices, use scripts/foyer-rauc-prod-ca instead: a root CA (kept
offline) that only ever signs a separate signing cert, so the signing key can
be rotated without re-flashing anything already deployed. See docs/RAUC.md
for the full setup and rotation story.

Building and installing an update
---------------------------------

  kas build kas/base.yml                                # image
  kas shell kas/base.yml -c "bitbake foyer-bundle"      # .raucb bundle

Copy the bundle to the device and install it. RAUC writes the inactive root
slot and its paired seed slot together, and grub boots the new slot once; if
the boot never reaches foyer-boot-confirm.service (which runs
`rauc status mark-good`), the next boot falls back to the previous slot.

Caveat worth knowing: because /etc is an overlay that survives updates, a file
changed in a new OS version stays masked by the copy in the old upper layer.
overlayfs-etc.bbclass explicitly leaves /etc migration out of scope.


III. Misc
=========

meta-foyer is the Yocto layer for Foyer OS, a headless Linux distribution
for a home server. It provides the foyer distro policy
(conf/distro/foyer.conf), the foyer-x86-64 machine
(conf/machine/foyer-x86-64.conf) and the foyer-image / foyer-image-dev image
recipes (recipes-foyer/images/).
