package com.roldanmatias.rickverse.core.domain.di

import javax.inject.Qualifier

/** Qualifier for the REST implementation of a repository. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class Rest

/** Qualifier for the GraphQL implementation of a repository. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class GraphQl
