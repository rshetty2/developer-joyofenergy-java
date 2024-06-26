package uk.tw.energy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.Consumption;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PricePlanService {


    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
    }

    public Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);



        if (!electricityReadings.isPresent()) {
            return Optional.empty();
        }

        //calculate the average reading , time elapsed, average consumption
        BigDecimal average = calculateAverageReading(electricityReadings.get());
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings.get());
        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        Consumption consumption = new Consumption(average,timeElapsed,averagedCost);

        System.out.println("*** size of readings *** " + electricityReadings.get().size());

        return Optional.of(pricePlans.stream().collect(
                Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(consumption, t))));
    }

    private BigDecimal calculateCost(Consumption consumption, PricePlan pricePlan) {

        BigDecimal cost = consumption.avgConsumptionPerHour().multiply(pricePlan.getUnitRate());

        System.out.printf("Price Plan : %s, price Plan unit rate :%f, Cost :%f\n",pricePlan.getPlanName(),pricePlan.getUnitRate().doubleValue(),cost.doubleValue());

        //return averagedCost.multiply(pricePlan.getUnitRate());
        return cost;
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::reading)
                .reduce(BigDecimal.ZERO, (reading, accumulator) -> reading.add(accumulator));

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
            ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::time))
                .get();

            ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::time))
                .get();

        return BigDecimal.valueOf(Duration.between(first.time(), last.time()).getSeconds() / 3600.0);
    }

}
