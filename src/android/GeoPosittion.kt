package cordova.plugin.prosegur.geolocation

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class GeoPosittion(
    private val DateTimeMobile: Date,
    private var Latitude: Double,
    private var Longitude: Double,
    private var UserId: String?,
    private var CenterId: String?,
    private var ProvenanceId: Int,
    private var GeoLocationTypeId: Int
) {
    private var country: String? = null
    private var imei: String? = null
    private var DateTime: String
    private var message: String? = null

    init {
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        this.DateTime = formatter.format(DateTimeMobile)
    }

    fun getLongitude(): Double = Longitude
    fun setLongitude(longitude: Double) {
        Longitude = longitude
    }

    fun getDateTime(): String = DateTime

    fun getDateTimeMobile(): Date = DateTimeMobile

    fun setDateTime(dateTime: String) {
        DateTime = dateTime
    }

    fun getLatitude(): Double = Latitude
    fun setLatitude(latitude: Double) {
        Latitude = latitude
    }

    fun getUserId(): String? = UserId
    fun setUserId(userId: String?) {
        UserId = userId
    }

    fun getProvenanceId(): Int = ProvenanceId
    fun setProvenance(provenanceId: Int) {
        ProvenanceId = provenanceId
    }

    fun getGeoLocationTypeId(): Int = GeoLocationTypeId

    fun getCenterId(): String? = CenterId
    fun setCenterId(centerId: String?) {
        CenterId = centerId
    }

    fun getCountry(): String? = country
    fun setCountry(country: String?) {
        this.country = country
    }

    fun getImei(): String? = imei
    fun setImei(imei: String?) {
        this.imei = imei
    }

    fun getMessage(): String? = message
    fun setMessage(message: String?) {
        this.message = message
    }
}