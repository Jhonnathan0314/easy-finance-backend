package com.easyfinance.shared.infrastructure.observability;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void usesValidCorrelationIdFromHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "trace_123-abc.def");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isEqualTo("trace_123-abc.def")
        );

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("trace_123-abc.def");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNotBlank()
        );

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank()
                .satisfies(value -> assertThat(UUID.fromString(value)).isNotNull());
    }

    @Test
    void generatesCorrelationIdWhenHeaderContainsUnsafeCharacters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "bad value\nwith-break");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNotEqualTo("bad value\nwith-break")
        );

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank()
                .satisfies(value -> assertThat(UUID.fromString(value)).isNotNull());
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsTooLong() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "a".repeat(101));

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).hasSize(36)
        );

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).hasSize(36);
    }
}

