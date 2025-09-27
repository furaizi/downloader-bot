package com.download.downloaderbot.infra.process.tools

import org.springframework.beans.factory.annotation.Qualifier

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS
)
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForYtDlp

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS
)
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForGalleryDl

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS
)
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ForInstaloader