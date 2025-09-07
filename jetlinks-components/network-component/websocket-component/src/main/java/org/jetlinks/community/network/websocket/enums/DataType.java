package org.jetlinks.community.network.websocket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataType {
    PROPERTY(0),
    WAVEFORM(1);

    private final int value;

    public static DataType of(int value) {
        for (DataType dataType : values()) {
            if (dataType.value == value) {
                return dataType;
            }
        }
        return PROPERTY;
    }
}
