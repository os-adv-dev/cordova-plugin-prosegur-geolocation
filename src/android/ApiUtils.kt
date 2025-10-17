package cordova.plugin.prosegur.geolocation

object ApiUtils {

    fun getAPIService(url: String = "https://placeholder-base-url.com/"): APIService {
        return RetrofitClient.getClient(url).create(APIService::class.java)
    }
}