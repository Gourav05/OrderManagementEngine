package com.peerisland.orderengine;

import com.peerisland.orderengine.dto.response.ErrorResponse;
import com.peerisland.orderengine.exception.GlobalExceptionHandler;
import com.peerisland.orderengine.exception.InvalidOrderStateException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesInvalidTransitionAsBadRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/orders/1/status");
        InvalidOrderStateException ex = new InvalidOrderStateException("Invalid status transition from A to B");

        ResponseEntity<ErrorResponse> resp = handler.handleInvalidTransition(ex, req);
        assertEquals(400, resp.getStatusCodeValue());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("INVALID_TRANSITION", body.getCode());
        assertEquals("/api/orders/1/status", body.getPath());
    }

    @Test
    void handlesNotFound() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/orders/does-not-exist");
        RuntimeException ex = new RuntimeException("not found");
        // We call the generic unexpected handler to ensure it returns 500
        ResponseEntity<ErrorResponse> resp = handler.handleUnexpected(ex, req);
        assertEquals(500, resp.getStatusCodeValue());
        assertEquals("INTERNAL_ERROR", resp.getBody().getCode());
    }
}
