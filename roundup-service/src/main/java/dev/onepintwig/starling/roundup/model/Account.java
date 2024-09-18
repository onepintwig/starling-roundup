package dev.onepintwig.starling.roundup.model;

import java.util.UUID;

public record Account (
        UUID accountUid, //This didn't work for ages as I spelt it accountUuid
        UUID defaultCategory
) {
}
