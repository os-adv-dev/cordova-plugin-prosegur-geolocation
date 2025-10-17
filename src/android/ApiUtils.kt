package cordova.plugin.prosegur.geolocation

object ApiUtils {

    fun getAPIService(url: String): APIService {
        return RetrofitClient.getClient(url).create(APIService::class.java)
    }
}