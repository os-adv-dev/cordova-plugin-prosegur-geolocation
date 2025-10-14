package cordova.plugin.prosegur.battery;

public class ApiUtils {
    private ApiUtils() {}

    public static APIService getAPIService(String url) {

        return RetrofitClient.getClient(url).create(APIService.class);
    }
}
