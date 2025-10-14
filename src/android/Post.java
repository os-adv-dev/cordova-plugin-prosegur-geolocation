package cordova.plugin.prosegur.geolocation;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class Post {

    @SerializedName("message")
    @Expose
    private String message;


    public String getCountry() {
        return message;
    }

    public void setCountry(String message) {
        this.message = message;
    }



}