package com.yzh.yingshi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EzvizDeviceResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesArrayDataResponse() throws Exception {
        var root = objectMapper.readTree("""
                {"code":"200","data":[{"deviceSerial":"A1"}]}
                """);

        assertEquals("A1", EzvizDeviceResponseParser.parseDevices(root).get(0).path("deviceSerial").asText());
    }

    @Test
    void parsesNestedListResponse() throws Exception {
        var root = objectMapper.readTree("""
                {"code":"200","data":{"list":[{"deviceSerial":"B2"}],"page":{"total":1}}}
                """);

        assertEquals("B2", EzvizDeviceResponseParser.parseDevices(root).get(0).path("deviceSerial").asText());
    }
}
