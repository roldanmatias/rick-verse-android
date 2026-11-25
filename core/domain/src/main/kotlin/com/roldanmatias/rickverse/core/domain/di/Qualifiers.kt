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

/** Qualifier for "whichever network backend is active" (REST or GraphQL), before the offline cache wraps it. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class Remote
