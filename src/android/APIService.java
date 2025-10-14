package cordova.plugin.prosegur.battery;

import java.util.ArrayList;

import cordova.plugin.prosegur.battery.Post;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface APIService {

    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @POST("/DeviceInfo_API/rest/DeviceInfo/SetDeviceInfo")
    Call<BatteryLevel> sendPost(@Header("Token") String token, @Body BatteryLevel body);

    @POST("/GeoLocation_API/rest/DeviceGeoLocations/SetDeviceGeoLocations")
    Call<GeoPosittion> sendPostGeo(@Header("Token") String token, @Query("country") String country, @Query("imei") String imei, @Body ArrayList<GeoPosittion> body);

    @POST("/DeviceInfo_API/rest/DeviceInfo/DeviceTurnOff")
    Call<BatteryLevel> sendPostOff(@Header("Token") String token, @Body BatteryLevel body);

}
