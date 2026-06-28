package com.example.hotelbooking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingSummaryEndpointTests {

    @Value("${local.server.port}")
    private int port;

    @Test
    void summaryReturnsBookingCountsAndRevenue() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/bookings/summary"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"totalBookings\":2")
                .contains("\"confirmedBookings\":2")
                .contains("\"checkedInBookings\":0")
                .contains("\"checkedOutBookings\":0")
                .contains("\"cancelledBookings\":0")
                .contains("\"upcomingBookings\":2")
                .contains("\"totalRevenue\":1200");
    }
}
