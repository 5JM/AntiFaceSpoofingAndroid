package aero.cubox.communication;

import aero.cubox.communication.utils.State;

public interface ProcessListener {
    void state_result(State state);
    void liveness_result(State state);
}