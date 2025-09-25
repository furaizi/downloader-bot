package com.download.downloaderbot.infra.process.tools

import org.springframework.beans.factory.annotation.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForYtDlp

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForGalleryDl

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForInstaloader