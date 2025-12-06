package com.download.downloaderbot.infra.media.files

import java.nio.file.Files
import java.nio.file.Path

fun Path.createFile(name: String): Path = Files.createFile(resolve(name))

fun Path.createDirectory(name: String): Path = Files.createDirectory(resolve(name))
