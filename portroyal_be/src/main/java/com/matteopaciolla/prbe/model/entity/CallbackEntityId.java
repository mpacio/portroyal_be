package com.matteopaciolla.prbe.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackEntityId implements Serializable {

    private String matchKeyCode;
    private Long userId;
}
