package com.wuch.flight.service;

import com.wuch.coze.toolcall.BaseToolCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("changeBooking")
public class ChangeBookingService extends BaseToolCall<BookingTools.ChangeBookingDatesRequest, String> {
    @Autowired
    private FlightBookingService flightBookingService;

    // 如果不需要metaData，也可以重新call(T t)
    @Override
    protected String call(BookingTools.ChangeBookingDatesRequest request, Map<String, String> metaData) {
        flightBookingService.changeBooking(request.bookingNumber(), request.name(), request.date(), request.from(),
                request.to());
        return "处理成功";
    }
}
