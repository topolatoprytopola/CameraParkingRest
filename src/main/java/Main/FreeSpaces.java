package Main;

public class FreeSpaces {
    private Integer average;
    private Integer difference;
    public FreeSpaces(Integer average, Integer difference)
    {
        this.average = average;
        this.difference = difference;
    }

    public Integer getAverage() {
        return average;
    }

    public void setAverage(Integer average) {
        this.average = average;
    }

    public Integer getDifference() {
        return difference;
    }

    public void setDifference(Integer difference) {
        this.difference = difference;
    }
}
