package com.roldanmatias.rickverse.data.rest

import com.roldanmatias.rickverse.core.network.NetworkJson
import com.roldanmatias.rickverse.core.testing.Fixtures
import kotlinx.serialization.decodeFromString

internal inline fun <reified T> fixture(name: String): T = NetworkJson.default.decodeFromString(Fixtures.load(name))
