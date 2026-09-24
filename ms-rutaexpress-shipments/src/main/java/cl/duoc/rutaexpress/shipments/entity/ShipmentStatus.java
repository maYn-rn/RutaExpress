package cl.duoc.rutaexpress.shipments.entity;

import java.util.Map;
import java.util.Set;

public enum ShipmentStatus {

    CREADO,
    ACEPTADO,
    EN_TRANSITO,
    ENTREGADO,
    CANCELADO;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREADO, Set.of(ACEPTADO, CANCELADO),
            ACEPTADO, Set.of(EN_TRANSITO, CANCELADO),
            EN_TRANSITO, Set.of(ENTREGADO, CANCELADO),
            ENTREGADO, Set.of(),
            CANCELADO, Set.of()
    );

    public boolean canTransitionTo(ShipmentStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

}
