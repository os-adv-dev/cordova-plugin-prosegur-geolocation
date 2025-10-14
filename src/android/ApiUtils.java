package cordova.plugin.prosegur.geolocation;

public class ApiUtils {
    private ApiUtils() {}

    public static APIService getAPIService(String url) {

        return RetrofitClient.getClient(url).create(APIService.class);
    }
}
