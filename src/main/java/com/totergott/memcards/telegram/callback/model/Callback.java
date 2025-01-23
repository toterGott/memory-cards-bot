package com.totergott.memcards.telegram.callback.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Callback {
    protected CallbackSource source;
    private String data;
    private CallbackSource breadCrumb;
    private Integer pageNumber;

    public abstract String getActionCode();
    public abstract void setAction(String actionName);
}
