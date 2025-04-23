package com.wuch.flight.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.wuch.flight.data.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.NestedExceptionUtils;

import java.time.LocalDate;
import java.util.function.Function;

@Configuration
public class BookingTools {

	private static final Logger logger = LoggerFactory.getLogger(BookingTools.class);

	@Autowired
	private FlightBookingService flightBookingService;

	public record BookingDetailsRequest(String bookingNumber, String name) {
	}

	public record ChangeBookingDatesRequest(String bookingNumber, String name, String date, String from, String to) {
	}

	public record CancelBookingRequest(String bookingNumber, String name) {
	}

	@JsonInclude(Include.NON_NULL)
	public record BookingDetails(String bookingNumber, String name, String date, String bookingStatus,
			String from, String to, String bookingClass) {
	}

	@Bean
	public Function<BookingDetailsRequest, BookingDetails> getBookingDetails() {
		return request -> {
			try {
				return flightBookingService.getBookingDetails(request.bookingNumber(), request.name());
			}
			catch (Exception e) {
				logger.warn("Booking details: {}", NestedExceptionUtils.getMostSpecificCause(e).getMessage());
				return new BookingDetails(request.bookingNumber(), request.name(), null, null, null, null, null);
			}
		};
	}


	@Bean
	public Function<CancelBookingRequest, String> cancelBooking() {
		return request -> {
			flightBookingService.cancelBooking(request.bookingNumber(), request.name());
			return "处理成功";
		};
	}




}
