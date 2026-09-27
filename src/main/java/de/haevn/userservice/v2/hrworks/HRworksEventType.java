package de.haevn.userservice.v2.hrworks;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HRworksEventType {
    /**
     * Wird gefeuert, wenn eine neue Person in HRworks angelegt wird.
     * Signalisiert deinem Service: "Daten via GET nachladen und Account erstellen".
     */
    PERSON_CREATED("persons.person.created"),

    /**
     * Wird gefeuert, wenn sich Stammdaten (Name, Mail, Abteilung, Vorgesetzter) ändern.
     * Signalisiert deinem Service: "Daten nachladen und lokalen Account updaten".
     */
    PERSON_CHANGED("persons.person.changed"),

    /**
     * Wird gefeuert, wenn eine Person hart aus HRworks gelöscht wird.
     * Signalisiert deinem Service: "Diesen Account sofort sperren/löschen" (Kein GET mehr möglich!).
     */
    PERSON_DELETED("persons.person.deleted");

    private final String value;

    HRworksEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}