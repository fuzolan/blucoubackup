# BlucouBackup
I startet this in 2014 in a sleepless night. I tried different things as backup solution but some things are always bother me. I would not write this in the same manner as today, but the outcome works still well for me. My main goal was something that could place some amounts of data securely in multiple storages like cloud or other medias.

# Features
* Async encryption
* Block deduplication over client and backupsets
* Offline storage support (You can backup tb's of data but currently only use a 32gb usb stick for you incremental backup. The rest of the hdds lying in a safe place. )  
* Compression
* Works on windows, linux, mac, 
* File/Directory blacklists
* Meta data stored in real database
* File Versions
* Searchable metadata from all you clients
* Multithreaded in some areas
* Cleanup old versions or deleted files
* mysql / sqlite support
* should be realy collisionfree (crc32, md5, sha512 of all your data)
* Optionally stores data as jpeg, so amazon prime users could backup everything to amazons photo cloud without pay anything ( but pssst )
# What it's not
* No precise snapshot of a specific time
* Saves no metadata like read-only flag or owner or any kind of os specific things
* A backupsolution for a beginner
* A backupsolution for someone who hates databases ^^
* A backupsolution with a simple restore ( there is a possability that you must reassembly all your dusty storages)

# how i used it
I backed up my arm5 nas with bad package distro support, different Linux, Windows and one Mac-Client. I have some external Hdds full of chunks. I never deleted something from my backups. There is one database running on my Pi which also does other things. Some clients are backed up through a share, others have it's own little storage to write some data. At some point the metadata in the db will be dumped and secured with the keys in a save place. If a storage is too small, it will be saved in a good place. If there are too many medias then smaller ones will copied on newer bigger ones.

# Features that I wanted to implement 
* gui
* container support for smaller blocks
* direct support for different systems/hoster
* meta data search
* storage groups
* automatic multiple backup of important data to different storage groups
* more tests

# Getting started
comming soon 

# Licence
Not decided yet. For personal use it will be absolutely free. If there is a corporate interest involved then just contact me.

# Donation
If you find this project useful or you have saved some data with it then you could share a coffee or something. 
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/BeckFlorian)
