package cordova.plugin.prosegur.geolocation;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface APIService {

    @POST("/GeoLocation_API/rest/DeviceGeoLocations/SetDeviceGeoLocations")
    Call<GeoPosittion> sendPostGeo(@Header("Token") String token, @Query("country") String country, @Query("imei") String imei, @Body ArrayList<GeoPosittion> body);

}