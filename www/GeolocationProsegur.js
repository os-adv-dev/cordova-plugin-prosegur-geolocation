

var exec = require('cordova/exec');

exports.stop = function (success, error) {
    exec(success, error, 'GeolocationProsegur', 'stop');
};

exports.initGeo = function (token, dir, country, format, imei, time, center, user, provenance, geoLocationTypeId, success, error) {
    exec(success, error, 'GeolocationProsegur', 'initGeo', [token, dir, country, imei, time, center, user, provenance, geoLocationTypeId]);
};

exports.validateGeo = function (token, dir, country, imei, center, user, provenance, success, error) {
    exec(success, error, 'GeolocationProsegur', 'checkGeo', [token, dir, country, imei, center, user, provenance]);
};

exports.stopGeo = function (success, error) {
    exec(success, error, 'GeolocationProsegur', 'stopGeo');
};




