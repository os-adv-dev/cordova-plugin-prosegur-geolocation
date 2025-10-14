package cordova.plugin.prosegur.geolocation;

public class BatteryLevel {


    private String country;
    private String imei;
    private Integer batteryLevel;
    private String message;

    public BatteryLevel( String country, String imei, Integer batteryLevel){
        this.country = country;
        this.imei = imei;
        this.batteryLevel = batteryLevel;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
