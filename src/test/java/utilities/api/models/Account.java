package utilities.api.models;

import java.math.BigDecimal;

public record Account(int id, int customerId, String type, BigDecimal balance) { }
