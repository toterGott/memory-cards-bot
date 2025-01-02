package com.example.memcards.telegram.callback.model;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class Callback {
    private CallbackSource source;
    private String data;

    public abstract String getActionCode();
    public abstract void setAction(String actionName);
}
