package com.wuch.flight.controller;

import com.wuch.flight.service.BookingTools;
import com.wuch.flight.service.FlightBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class BookingController {

	private final FlightBookingService flightBookingService;

	@RequestMapping("/")
	public String index() {
		return "index";
	}

	@RequestMapping("/api/bookings")
	@ResponseBody
	public List<BookingTools.BookingDetails> getBookings() {
		return flightBookingService.getBookings();
	}
}
