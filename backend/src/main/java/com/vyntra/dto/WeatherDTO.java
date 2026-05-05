package com.vyntra.dto;

public class WeatherDTO {
    private String location;
    private Double temperature;
    private String condition;
    private Double humidity;
    private Double windSpeed;
    private boolean isRainy;
    private String icon;

    public WeatherDTO() {}

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }

    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }

    public boolean isRainy() { return isRainy; }
    public void setRainy(boolean rainy) { isRainy = rainy; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
