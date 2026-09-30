package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.service.cdn.dto.GameDto
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import retrofit2.http.GET
import retrofit2.http.Url

internal interface ContentApi {
    @GET
    suspend fun catalogue(@Url url: String): List<GameDto?>

    @GET
    suspend fun homeContent(@Url url: String): HomeContentDto
}
