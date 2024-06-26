package uk.tw.energy.domain;

import java.math.BigDecimal;

public record Consumption(BigDecimal avgReading, BigDecimal timeElapsed, BigDecimal avgConsumptionPerHour) {
}
