package com.totergott.memcards.telegram.callback.model;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Callback {
    protected CallbackSource source;
    private String data;
    private String additionalData;

    public abstract String getActionCode();
    public abstract void setAction(String actionName);

    public void setDate(UUID id) {
        this.data = id.toString();
    }
}
