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
 II. Misc


I. Adding the meta-foyer layer to your build
=================================================

meta-foyer is normally pulled in automatically as part of the foyer-os kas
project (see kas/base.yml in foyer-os). To add it to an existing bitbake
build manually instead, run:

  bitbake-layers add-layer meta-foyer

II. Misc
========

meta-foyer is the Yocto layer for Foyer OS, a headless Linux distribution
for a home server. It provides the foyer distro policy
(conf/distro/foyer.conf) and the foyer-image / foyer-image-dev image
recipes (recipes-foyer/images/).
