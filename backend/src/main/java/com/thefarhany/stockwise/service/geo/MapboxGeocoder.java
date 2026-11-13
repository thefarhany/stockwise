package com.thefarhany.stockwise.service.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thefarhany.stockwise.dto.geo.GeoPlaceResponse;
import com.thefarhany.stockwise.dto.outlet.AddressDto;
import com.thefarhany.stockwise.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Satu-satunya kelas yang berbicara dengan Mapbox Geocoding API v6 (search/geocode/v6). Token dibaca dari properti
 * `stockwise.mapbox.access-token` (env MAPBOX_ACCESS_TOKEN, lihat .env.example). Mengganti provider berarti
 * mengganti kelas ini saja.
 *
 * Token ikut di query string (aturan Mapbox), jadi URL dan pesan exception tidak pernah ditulis ke log.
 */
@Component
public class MapboxGeocoder {

	private static final Logger log = LoggerFactory.getLogger(MapboxGeocoder.class);

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Named(String name) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Context(Named address, Named street, Named neighborhood, Named postcode, Named locality, Named place,
			Named region) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Coordinates(BigDecimal latitude, BigDecimal longitude) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Properties(@JsonProperty("full_address") String fullAddress, String name, Coordinates coordinates,
			Context context) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Feature(Properties properties) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record FeatureCollection(List<Feature> features) {
	}

	private final RestClient http;
	private final String token;

	public MapboxGeocoder(@Value("${stockwise.mapbox.url}") String url,
			@Value("${stockwise.mapbox.access-token}") String token) {
		this.token = token == null ? "" : token.trim();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build());
		factory.setReadTimeout(Duration.ofSeconds(8));
		this.http = RestClient.builder().baseUrl(url).requestFactory(factory).build();
	}

	/** Alamat di titik itu (0 atau 1 hasil). */
	public List<GeoPlaceResponse> reverse(BigDecimal latitude, BigDecimal longitude) {
		return call("reverse", b -> b.path("/search/geocode/v6/reverse").queryParam("latitude", latitude)
				.queryParam("longitude", longitude).queryParam("limit", 1));
	}

	/** Alamat yang cocok dengan teks, di Indonesia; diurutkan lebih dekat ke titik `near` bila ada. */
	public List<GeoPlaceResponse> search(String query, BigDecimal nearLatitude, BigDecimal nearLongitude) {
		return call("search", b -> {
			b.path("/search/geocode/v6/forward").queryParam("q", query).queryParam("country", "id")
					.queryParam("limit", 5).queryParam("autocomplete", true);
			if (nearLatitude != null && nearLongitude != null) {
				b.queryParam("proximity", nearLongitude + "," + nearLatitude);
			}
			return b;
		});
	}

	private List<GeoPlaceResponse> call(String op, Function<UriBuilder, UriBuilder> uri) {
		if (token.isEmpty()) {
			throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "GEOCODING_UNAVAILABLE",
					"Pencarian alamat belum dikonfigurasi (MAPBOX_ACCESS_TOKEN kosong).");
		}
		FeatureCollection body;
		try {
			body = http.get()
					.uri(b -> uri.apply(b).queryParam("language", "id").queryParam("access_token", token).build())
					.retrieve().body(FeatureCollection.class);
		}
		catch (RestClientException ex) {
			log.warn("mapbox_failed op={} type={}", op, ex.getClass().getSimpleName());
			throw new BusinessException(HttpStatus.BAD_GATEWAY, "GEOCODING_FAILED", "Layanan alamat sedang gagal.");
		}
		return body == null || body.features() == null ? List.of()
				: body.features().stream().filter(f -> f.properties() != null).map(f -> toPlace(f.properties())).toList();
	}

	/**
	 * Bagian alamat Mapbox ke bagian alamat Indonesia: neighborhood = kelurahan/desa, locality = kecamatan,
	 * place = kota/kabupaten, region = provinsi. Teks dipotong sesuai panjang kolom outlet.
	 */
	static GeoPlaceResponse toPlace(Properties p) {
		Context c = p.context() == null ? new Context(null, null, null, null, null, null, null) : p.context();
		String street = c.address() != null ? c.address().name() : name(c.street());
		AddressDto address = new AddressDto(cut(p.fullAddress() != null ? p.fullAddress() : p.name(), 255),
				cut(street, 150), cut(name(c.neighborhood()), 100), cut(name(c.locality()), 100),
				cut(name(c.place()), 100), cut(name(c.region()), 100), cut(name(c.postcode()), 10));
		Coordinates xy = p.coordinates();
		return new GeoPlaceResponse(address, xy == null ? null : scale(xy.latitude()),
				xy == null ? null : scale(xy.longitude()));
	}

	private static String name(Named n) {
		return n == null ? null : n.name();
	}

	private static String cut(String v, int max) {
		return v == null || v.length() <= max ? v : v.substring(0, max);
	}

	// sama dengan kolom outlet DECIMAL(9,6)
	private static BigDecimal scale(BigDecimal v) {
		return v == null ? null : v.setScale(6, RoundingMode.HALF_UP);
	}

}
