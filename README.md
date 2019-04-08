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
* A backupsolution with a simple restore ( there is a possability that you must reassembly all your dusty storages )

# How I used it
I backed up my arm5 nas with bad package distro support, different Linux, Windows and one Mac-Client. I have some external Hdds full of chunks. I never deleted something from my backups. There is one database running on my Pi which also does other things. Some clients are backed up through a share, others have it's own little storage to write some data. At some point the metadata in the db will be dumped and secured with the keys in a save place. If a storage is too small, it will be saved in a good place. If there are too many medias then smaller ones will copied on newer bigger ones.

# Features that I wanted to implement 
* gui
* container support for smaller blocks
* direct support for different systems/hoster
* meta data search
* storage groups
* automatic multiple backup of important data to different storage groups
* Port -> Spring
* more tests

# Getting started
* Download it - https://github.com/fuzolan/blucoubackup/releases/download/v1.0/BlucouBackup.zip
* Extract it
* Start it with `java -jar BlucouBackup.jar`
* Generate a keyset with `java -jar BlucouBackup.jar -keygen` !!!only once if you share a database with another client!!!
* Edit the created `blucoubackup.properties` and adjust settings
* If you want to see whats going on, then you can adjust the log4j.properties, too
* Init a new Database `java -jar BlucouBackup.jar -setup_database_and_lose_old_data`
* Init a Storage `java -jar BlucouBackup.jar -s ABSOLUTEPATHTOSTORAGE`
* If you want to set a limit for the storage run '-quota'
* Make your first backup `java -jar -Xmx256m BlucouBackup.jar -b PATHTOSTORAGE`
* Put this command into a scheduler like crontab, windows task scheduler, ...
* Save your metadata in your db often and put your settings in a  
* If you want something restored then use `java -jar -Xmx256m BlucouBackup.jar -r PATHTORESTORE`
* You should always test every backupsolution with backup/restore if you get what you want
* enjoy

# Options
```
dbtype = type of database -> sqlite and mysql are supported -> mysql is prefered
host = mysql: hostname/ip sqlite: absolute path to databasefile
db = database name
user = database user (only mysql) 
saveAsPicture = true/false -> relevant if you want to obfuscate your data as pictures
pass = database pass (only mysql)
keyLengthInByte = length in bytes...you should take at least 2048 
blockSize = size in kib. if it's longer then the data will be cut in chunks
publicKey = relevant for encrypting data
privateKey = relevant for decryption. So if you have to restore something you need something here. the private key is protected by your password. if you are paranoid or something you can leave this blank and paste your private key just before you start a restore 
checkDirectoryIntervalInDays = if you don't want to traverse your whole tree everytime you can put a number here
keepDeletedInDays = if you run a cleanup then the files with deleted-flag will be discarded
keepObsoleteInDays = if you run a cleanup then the files with obsolete-flag will be discarded
compressionAlgo = 0:uncompressed, 1:zip, 2:zlib, 3:bzip2
compression = 1 (low compression) to 9 (high compression)  
dirBlacklist = comma seperated list of directories you don't want, you should also use wildcards.
fileBlacklist =  comma seperated list of files you don't want, you should also use wildcards
machine_identifier = you can put a client identifier here like 'windows_workstation_in_guest_toilet'  
```

# How to work on this
I used IntelliJ. You should run the Tests. There are no Unit-Tests but it's a functional test about backup/cleanup/restore. So start your debugger and you will see what's going on. Take also a look in the testdatabase.db (sqlite) and maybe the testdata which is written. Ah and of course you should know something about Java too.

# Used Libs
* BouncyCastle
* log4j
* ormlite
* commons-cli
* commons-io
* commons-logging
* commons-configuration
* sqlite-jdbc
* mysql-connector
* junit

# Donation
If you find this project useful or you have saved some data with it then you could share a coffee or something. 
[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/BeckFlorian)
