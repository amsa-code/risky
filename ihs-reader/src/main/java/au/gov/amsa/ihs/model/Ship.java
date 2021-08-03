package au.gov.amsa.ihs.model;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.davidmoten.guavamini.Preconditions;

public class Ship {

    private final String imo;
    private final Optional<String> mmsi;
    private final Optional<String> type2;
    private final Optional<String> type3;
    private final Optional<String> type4;
    private final Optional<String> type5;
    private final Optional<Long> grossTonnage;
    private final Optional<String> classificationSocietyCode;
    private final Optional<String> flagCode;
    private final Optional<String> groupBeneficialOwnerCompanyCode;
    private final Optional<String> groupBeneficialOwnerCountryOfDomicileCode;
    private final Optional<String> countryOfBuildCode;
    private final Optional<Integer> yearOfBuild;
    private final Optional<Integer> monthOfBuild;
    private final Optional<Float> deadweightTonnage;
    private final Optional<String> statCode5;
    private final Optional<Float> lengthOverallMetres;
    private final Optional<Float> breadthMetres;
    private final Optional<Float> displacementTonnage;
    private final Optional<Float> draughtMetres;
    private final Optional<Float> speedKnots;
    private final Optional<OffsetDateTime> lastUpdateTime;
    private final Optional<String> name;
    private final Optional<String> shipBuilderCompanyCode;

    private Ship(String imo, Optional<String> mmsi, Optional<String> type2, Optional<String> type3,
            Optional<String> type4, Optional<String> type5, Optional<Long> grossTonnage,
            Optional<String> classificationSocietyCode, Optional<String> flagCode,
            Optional<String> groupBeneficialOwnerCompanyCode,
            Optional<String> groupBeneficialOwnerCountryOfDomicileCode,
            Optional<String> countryOfBuildCode, Optional<Integer> yearOfBuild,
            Optional<Integer> monthOfBuild, Optional<Float> deadweightTonnage,
            Optional<String> statCode5, Optional<Float> lengthOverallMetres,
            Optional<Float> breadthMetres, Optional<Float> displacementTonnage,
            Optional<Float> draughtMetres, Optional<Float> speedKnots,
            Optional<OffsetDateTime> lastUpdateTime, Optional<String> name,
            Optional<String> shipBuilderCompanyCode) {
        Preconditions.checkNotNull(imo);
        Preconditions.checkNotNull(mmsi);
        Preconditions.checkNotNull(type2);
        Preconditions.checkNotNull(type3);
        Preconditions.checkNotNull(type4);
        Preconditions.checkNotNull(type5);
        Preconditions.checkNotNull(grossTonnage);
        Preconditions.checkNotNull(classificationSocietyCode);
        Preconditions.checkNotNull(flagCode);
        Preconditions.checkNotNull(groupBeneficialOwnerCompanyCode);
        Preconditions.checkNotNull(groupBeneficialOwnerCountryOfDomicileCode);
        Preconditions.checkNotNull(countryOfBuildCode);
        Preconditions.checkNotNull(yearOfBuild);
        Preconditions.checkNotNull(monthOfBuild);
        Preconditions.checkNotNull(deadweightTonnage);
        Preconditions.checkNotNull(statCode5);
        Preconditions.checkNotNull(lengthOverallMetres);
        Preconditions.checkNotNull(breadthMetres);
        Preconditions.checkNotNull(displacementTonnage);
        Preconditions.checkNotNull(draughtMetres);
        Preconditions.checkNotNull(speedKnots);
        Preconditions.checkNotNull(lastUpdateTime);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(shipBuilderCompanyCode);
        this.imo = imo;
        this.mmsi = mmsi;
        this.type2 = type2;
        this.type3 = type3;
        this.type4 = type4;
        this.type5 = type5;
        this.grossTonnage = grossTonnage;
        this.classificationSocietyCode = classificationSocietyCode;
        this.flagCode = flagCode;
        this.groupBeneficialOwnerCompanyCode = groupBeneficialOwnerCompanyCode;
        this.groupBeneficialOwnerCountryOfDomicileCode = groupBeneficialOwnerCountryOfDomicileCode;
        this.countryOfBuildCode = countryOfBuildCode;
        this.yearOfBuild = yearOfBuild;
        this.monthOfBuild = monthOfBuild;
        this.deadweightTonnage = deadweightTonnage;
        this.statCode5 = statCode5;
        this.lengthOverallMetres = lengthOverallMetres;
        this.breadthMetres = breadthMetres;
        if (displacementTonnage.isPresent() && displacementTonnage.get() == 0)
            this.displacementTonnage = Optional.empty();
        else
            this.displacementTonnage = displacementTonnage;
        this.draughtMetres = draughtMetres;
        this.speedKnots = speedKnots;
        this.lastUpdateTime = lastUpdateTime;
        this.name = name;
        this.shipBuilderCompanyCode = shipBuilderCompanyCode;
    }

    public Optional<String> getName() {
        return name;
    }

    public String getImo() {
        return imo;
    }

    public Optional<String> getMmsi() {
        return mmsi;
    }

    public Optional<String> getType2() {
        return type2;
    }

    public Optional<String> getType3() {
        return type3;
    }

    public Optional<String> getType4() {
        return type4;
    }

    public Optional<String> getType5() {
        return type5;
    }

    public Optional<Long> getGrossTonnage() {
        return grossTonnage;
    }

    public Optional<String> getClassificationSocietyCode() {
        return classificationSocietyCode;
    }

    public Optional<String> getFlagCode() {
        return flagCode;
    }

    public Optional<String> getGroupBeneficialOwnerCompanyCode() {
        return groupBeneficialOwnerCompanyCode;
    }

    public Optional<String> getGroupBeneficialOwnerCountryOfDomicileCode() {
        return groupBeneficialOwnerCountryOfDomicileCode;
    }

    public Optional<String> getCountryOfBuildCode() {
        return countryOfBuildCode;
    }

    public Optional<Integer> getYearOfBuild() {
        return yearOfBuild;
    }

    public Optional<Integer> getMonthOfBuild() {
        return monthOfBuild;
    }

    public Optional<Float> getDeadweightTonnage() {
        return deadweightTonnage;
    }

    public Optional<String> getStatCode5() {
        return statCode5;
    }

    public Optional<Float> getLengthOverallMetres() {
        return lengthOverallMetres;
    }

    public Optional<Float> getBreadthMetres() {
        return breadthMetres;
    }

    public Optional<Float> getDisplacementTonnage() {
        return displacementTonnage;
    }

    public Optional<Float> getDraughtMetres() {
        return draughtMetres;
    }

    public Optional<Float> getSpeedKnots() {
        return speedKnots;
    }

    public Optional<OffsetDateTime> getLastUpdateTime() {
        return lastUpdateTime;
    }

    public Optional<String> getShipBuilderCompanyCode() {
        return shipBuilderCompanyCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String imo;
        private Optional<String> mmsi;
        private Optional<String> type2;
        private Optional<String> type3;
        private Optional<String> type4;
        private Optional<String> type5;
        private Optional<Long> grossTonnage;
        private Optional<String> classificationSocietyCode;
        private Optional<String> flagCode;
        private Optional<String> groupBeneficialOwnerCompanyCode;
        private Optional<String> groupBeneficialOwnerCountryOfDomicileCode;
        private Optional<String> countryOfBuildCode;
        private Optional<Integer> yearOfBuild;
        private Optional<Integer> monthOfBuild;
        private Optional<Float> deadweightTonnage;
        private Optional<String> statCode5;
        private Optional<Float> lengthOverallMetres;
        private Optional<Float> breadthMetres;
        private Optional<Float> displacementTonnage;
        private Optional<Float> draughtMetres;
        private Optional<Float> speedKnots;
        private Optional<OffsetDateTime> lastUpdateTime;
        private Optional<String> name;
        private Optional<String> shipBuilderCompanyCode;

        private Builder() {
        }

        public Builder imo(String imo) {
            this.imo = imo;
            return this;
        }

        public Builder mmsi(Optional<String> mmsi) {
            this.mmsi = mmsi;
            return this;
        }

        public Builder type2(Optional<String> type2) {
            this.type2 = type2;
            return this;
        }

        public Builder type3(Optional<String> type3) {
            this.type3 = type3;
            return this;
        }

        public Builder type4(Optional<String> type4) {
            this.type4 = type4;
            return this;
        }

        public Builder type5(Optional<String> type5) {
            this.type5 = type5;
            return this;
        }

        public Builder grossTonnage(Optional<Long> grossTonnage) {
            this.grossTonnage = grossTonnage;
            return this;
        }

        public Builder classificationSocietyCode(Optional<String> classificationSocietyCode) {
            this.classificationSocietyCode = classificationSocietyCode;
            return this;
        }

        public Builder flagCode(Optional<String> flagCode) {
            this.flagCode = flagCode;
            return this;
        }

        public Builder groupBeneficialOwnerCompanyCode(
                Optional<String> groupBeneficialOwnerCompanyCode) {
            this.groupBeneficialOwnerCompanyCode = groupBeneficialOwnerCompanyCode;
            return this;
        }

        public Builder groupBeneficialOwnerCountryOfDomicileCode(
                Optional<String> groupBeneficialOwnerCountryOfDomicileCode) {
            this.groupBeneficialOwnerCountryOfDomicileCode = groupBeneficialOwnerCountryOfDomicileCode;
            return this;
        }

        public Builder countryOfBuildCode(Optional<String> countryOfBuildCode) {
            this.countryOfBuildCode = countryOfBuildCode;
            return this;
        }

        public Builder yearOfBuild(Optional<Integer> yearOfBuild) {
            this.yearOfBuild = yearOfBuild;
            return this;
        }

        public Builder monthOfBuild(Optional<Integer> monthOfBuild) {
            this.monthOfBuild = monthOfBuild;
            return this;
        }

        public Builder deadweightTonnage(Optional<Float> dwt) {
            this.deadweightTonnage = dwt;
            return this;
        }

        public Builder statCode5(Optional<String> value) {
            this.statCode5 = value;
            return this;
        }

        public Builder lengthOverallMetres(Optional<Float> value) {
            this.lengthOverallMetres = value;
            return this;
        }

        public Builder breadthMetres(Optional<Float> value) {
            this.breadthMetres = value;
            return this;
        }

        public Builder displacementTonnage(Optional<Float> value) {
            this.displacementTonnage = value;
            return this;
        }

        public Builder draughtMetres(Optional<Float> value) {
            this.draughtMetres = value;
            return this;
        }

        public Builder speedKnots(Optional<Float> value) {
            this.speedKnots = value;
            return this;
        }

        public Builder lastUpdateTime(Optional<OffsetDateTime> value) {
            this.lastUpdateTime = value;
            return this;
        }

        public Builder name(Optional<String> name) {
            this.name = name;
            return this;
        }

        public Builder shipBuilderCompanyCode(Optional<String> shipBuilderCompanyCode) {
            this.shipBuilderCompanyCode = shipBuilderCompanyCode;
            return this;
        }

        public Ship build() {
            return new Ship(imo, mmsi, type2, type3, type4, type5, grossTonnage,
                    classificationSocietyCode, flagCode, groupBeneficialOwnerCompanyCode,
                    groupBeneficialOwnerCountryOfDomicileCode, countryOfBuildCode, yearOfBuild,
                    monthOfBuild, deadweightTonnage, statCode5, lengthOverallMetres, breadthMetres,
                    displacementTonnage, draughtMetres, speedKnots, lastUpdateTime, name,
                    shipBuilderCompanyCode);
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Ship [imo=");
        b.append(imo);
        b.append(", mmsi=");
        b.append(mmsi);
        b.append(", type2=");
        b.append(type2);
        b.append(", type3=");
        b.append(type3);
        b.append(", type4=");
        b.append(type4);
        b.append(", type5=");
        b.append(type5);
        b.append(", grossTonnage=");
        b.append(grossTonnage);
        b.append(", classificationSocietyCode=");
        b.append(classificationSocietyCode);
        b.append(", flagCode=");
        b.append(flagCode);
        b.append(", groupBeneficialOwnerCompanyCode=");
        b.append(groupBeneficialOwnerCompanyCode);
        b.append(", groupBeneficialOwnerCountryOfDomicileCode=");
        b.append(groupBeneficialOwnerCountryOfDomicileCode);
        b.append(", countryOfBuildCode=");
        b.append(countryOfBuildCode);
        b.append(", yearOfBuild=");
        b.append(yearOfBuild);
        b.append(", monthOfBuild=");
        b.append(monthOfBuild);
        b.append(", deadweightTonnage=");
        b.append(deadweightTonnage);
        b.append(", statCode5=");
        b.append(statCode5);
        b.append(", lengthOverallMetres=");
        b.append(lengthOverallMetres);
        b.append(", breadthMetres=");
        b.append(breadthMetres);
        b.append(", displacementTonnage=");
        b.append(displacementTonnage);
        b.append(", draughtMetres=");
        b.append(draughtMetres);
        b.append(", speedKnots=");
        b.append(speedKnots);
        b.append(", lastUpdateTime=");
        b.append(lastUpdateTime);
        b.append(", name=");
        b.append(name);
        b.append(", shipBuilderCompanyCode=");
        b.append(shipBuilderCompanyCode);
        b.append("]");
        return b.toString();
    }

}
