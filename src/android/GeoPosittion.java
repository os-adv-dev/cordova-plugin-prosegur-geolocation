package cordova.plugin.prosegur.battery;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.security.acl.AclEntry;
import java.util.Date;
import java.util.TimeZone;

public class GeoPosittion {

    private String country;
    private String imei;
    private String DateTime;
    private double Latitude;
    private double Longitude;
    private String UserId;
    private String CenterId;
	private Date DateTimeMobile;
	private int ProvenanceId;
	private int GeoLocationTypeId;

    private String message;

    public GeoPosittion(Date DateTimeMobile, double Latitude, double Longitude, String UserId, String CenterId, int provenanceId, int geoLocationTypeId){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		this.DateTimeMobile = DateTimeMobile;
        this.DateTime = formatter.format(DateTimeMobile);
        //this.DateTime = DateTime;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.UserId = UserId;
        this.CenterId = CenterId;
		this.ProvenanceId = provenanceId;
		this.GeoLocationTypeId = geoLocationTypeId;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public String getDateTime() {
        return DateTime;
    }
	
	public Date getDateTimeMobile() {
        return DateTimeMobile;
    }

    public void setDateTime(String dateTime) {
        DateTime = dateTime;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }
	
	public int getProvenanceId() {
        return ProvenanceId;
    }
	
	public int getGeoLocationTypeId() {
        return GeoLocationTypeId;
    }

    public void setProvenance(int provenanceId) {
        ProvenanceId = provenanceId;
    }
	

    public String getCenterId() {
        return CenterId;
    }

    public void setCenterId(String centerId) {
        CenterId = centerId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
