package com.pcare.casefile.domain;

/** Operational priority. EMERGENCY cases surface at the top of the Command Centre and
 * trigger the configurable emergency escalation path (never a substitute for 112/emergency
 * medical services). */
public enum CasePriority {
    LOW,
    NORMAL,
    HIGH,
    EMERGENCY
}
