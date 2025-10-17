package cordova.plugin.prosegur.geolocation

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface APIService {

    @POST
    suspend fun sendPostGeo(
        @Url fullUrl: String,
        @Header("Token") token: String,
        @Query("country") country: String,
        @Query("imei") imei: String,
        @Body body: ArrayList<GeoPosittion> = arrayListOf()
    ): GeoPosittion
}