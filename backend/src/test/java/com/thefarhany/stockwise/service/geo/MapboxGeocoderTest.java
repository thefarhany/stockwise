package com.thefarhany.stockwise.service.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thefarhany.stockwise.dto.geo.GeoPlaceResponse;
import com.thefarhany.stockwise.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class MapboxGeocoderTest {

	// Bentuk respons Mapbox Geocoding v6 (dipangkas) untuk satu alamat di Jakarta.
	private static final String RESPONSE = """
			{"type":"FeatureCollection","features":[{"type":"Feature","properties":{
			  "feature_type":"address","name":"Jalan Kebon Sirih 12",
			  "full_address":"Jalan Kebon Sirih 12, Kebon Sirih, Menteng, Jakarta Pusat, DKI Jakarta 10340, Indonesia",
			  "coordinates":{"longitude":106.8271234567,"latitude":-6.1834567891},
			  "context":{
			    "address":{"name":"Jalan Kebon Sirih 12","address_number":"12","street_name":"Jalan Kebon Sirih"},
			    "street":{"name":"Jalan Kebon Sirih"},"neighborhood":{"name":"Kebon Sirih"},
			    "postcode":{"name":"10340"},"locality":{"name":"Menteng"},"place":{"name":"Jakarta Pusat"},
			    "region":{"name":"DKI Jakarta","region_code":"JK"},"country":{"name":"Indonesia","country_code":"ID"}}}}]}""";

	@Test
	void mapsMapboxPartsToIndonesianAddressParts() {
		MapboxGeocoder.FeatureCollection body = JsonMapper.builder().build().readValue(RESPONSE,
				MapboxGeocoder.FeatureCollection.class);
		GeoPlaceResponse place = MapboxGeocoder.toPlace(body.features().get(0).properties());

		assertThat(place.address().full()).startsWith("Jalan Kebon Sirih 12, Kebon Sirih");
		assertThat(place.address().street()).isEqualTo("Jalan Kebon Sirih 12");
		assertThat(place.address().village()).isEqualTo("Kebon Sirih");
		assertThat(place.address().district()).isEqualTo("Menteng");
		assertThat(place.address().city()).isEqualTo("Jakarta Pusat");
		assertThat(place.address().province()).isEqualTo("DKI Jakarta");
		assertThat(place.address().postalCode()).isEqualTo("10340");
		// dibulatkan ke DECIMAL(9,6) kolom outlet
		assertThat(place.latitude()).isEqualByComparingTo("-6.183457");
		assertThat(place.longitude()).isEqualByComparingTo("106.827123");
	}

	@Test
	void missingContextStillGivesTheFullAddress() {
		GeoPlaceResponse place = MapboxGeocoder.toPlace(
				new MapboxGeocoder.Properties(null, "Monas", new MapboxGeocoder.Coordinates(BigDecimal.ONE, BigDecimal.TEN), null));
		assertThat(place.address().full()).isEqualTo("Monas");
		assertThat(place.address().street()).isNull();
	}

	@Test
	void blankTokenIsNotConfiguredAndNeverCallsOut() {
		// alamat sengaja tidak bisa dijangkau: bila kode mencoba menelepon, pesannya akan GEOCODING_FAILED
		MapboxGeocoder geocoder = new MapboxGeocoder("http://127.0.0.1:1", " ");
		assertThatThrownBy(() -> geocoder.reverse(BigDecimal.ONE, BigDecimal.ONE)).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).code()).isEqualTo("GEOCODING_UNAVAILABLE");
	}

	@Test
	void unreachableServiceFailsWithoutLeakingTheToken() {
		MapboxGeocoder geocoder = new MapboxGeocoder("http://127.0.0.1:1", "pk.rahasia");
		assertThatThrownBy(() -> geocoder.search("kebon sirih", null, null)).isInstanceOf(BusinessException.class)
				.satisfies(e -> {
					assertThat(((BusinessException) e).code()).isEqualTo("GEOCODING_FAILED");
					assertThat(e.getMessage()).doesNotContain("pk.rahasia");
				});
	}

}
