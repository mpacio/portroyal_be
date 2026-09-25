package com.matteopaciolla.portroyal.core.cards.enums;

import com.matteopaciolla.portroyal.core.cards.employees.*;
import lombok.Getter;

@Getter
public enum ExpeditionEmployee {
    CAPTAIN(Captain.class),
    PRIEST(Priest.class),
    SETTLER(Settler.class),
    HANDYMAN(Handyman.class),;

    private final Class<? extends EmployeeCard> referredClass;

    ExpeditionEmployee(Class<? extends EmployeeCard> referredClass) {
        this.referredClass = referredClass;
    }
}
