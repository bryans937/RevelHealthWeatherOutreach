package acme.outreach;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import com.google.gson.*;

/**
 * A simple class that will gather the weather forecast for the next 5 days in Minneapolis from openweathermap.org,
 * and using a set of criteria will determine the best way of reaching out to potential customers for those days.
 *
 * Based on a Revel Health Software Engineer practical exercise / coding assessment.
 * @author Bryan Simpson, date 10/25/2019.
 */
public class WeatherOutreach {

    // API key to connect to OpenWeatherMap
    private static final String apiKey = "cb95bbb4938898960b38786f8eb6e112";

    // query URL for Minneapolis
    private static final String cityForecastURL = "http://api.openweathermap.org/data/2.5/forecast?q=Minneapolis,us";

    /**
     * Gets the raw weather data from openweathermap.org and returns it as a formatted Json object
     * @return formatted Json object
     * @throws MalformedURLException
     */
    private static JsonObject getWeatherData() throws MalformedURLException {
        // setup the connection
        URL completeURL = new URL(cityForecastURL + "&appid=" + apiKey + "&units=imperial");
        StringBuffer result = new StringBuffer();

        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) completeURL.openConnection();
            connection.setRequestMethod("GET");

            // read contents
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String nextLine = reader.readLine();

            while(nextLine != null) {
                result.append(nextLine);
                nextLine = reader.readLine();
            }

            // disconnect and return
            connection.disconnect();
        } catch (IOException e) {
            System.out.println("Caught IO exception in getWeatherData: " + e);
        }

        // save info to Json, format and return it
        Gson builder = new GsonBuilder().setPrettyPrinting().create();
        JsonObject weatherData = builder.fromJson(result.toString(), JsonObject.class);

        System.out.println(weatherData.toString());
        return weatherData;
    }

    /**
     * Parses the Json Weather date to determine the average temperature and weather conditions for a given day, uses that
     * information to determine the best outreach method for that day, and returns a HashMap of <Date, Outreach Method>.
     *
     * @return HashMap<String, String> of <Date, OutreachMethod>.
     */
    public static HashMap<String, String> getForecast() {
        HashMap<String, String> forecast = new HashMap<String, String>();

        JsonObject dailyWeather;
        try {
            dailyWeather = getWeatherData();
        } catch (IOException e) {
            System.out.println("Caught IO exception in GetForecast: " + e);
            return null;
        }
        JsonArray data = dailyWeather.getAsJsonArray("list");

        int tempTotals = 0;
        int tempCount = 0;
        boolean rainy = false;
        boolean sunny = false;

        // iterate through all the entries, knowing that they are sorted chronologically
        // save up the temperature readings for a given day to calculate the average and if rain will occur at any time
        for (int i = 0; i < data.size()-1; i++) {
            JsonObject currentObject = data.get(i).getAsJsonObject();
            JsonObject nextObject = data.get(i+1).getAsJsonObject();
            String dateText = currentObject.get("dt_txt").getAsString().substring(0, 10);
            String nextDateText = nextObject.get("dt_txt").getAsString().substring(0, 10);

            String simpleDate;
            String nextSimpleDate;

            // try parsing the dates
            try {
                simpleDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateText).toString();
                nextSimpleDate = new SimpleDateFormat("yyyy-MM-dd").parse(nextDateText).toString();
            } catch (ParseException e) {
                System.out.println("Caught Parse exception in GetForecast: " + e);
                return null;
            }

            // Save off current date info (temperature and weather condition)
            JsonObject currentMain = currentObject.get("main").getAsJsonObject();
            String currentWeather = currentObject.getAsJsonArray("weather").get(0).getAsJsonObject().get("main").getAsString();

            tempTotals += currentMain.get("temp").getAsInt();
            tempCount+=1;
            if (currentWeather == "Rainy") {
                rainy = true;
            } else if (currentWeather == "Clear") {
                sunny = true;
            }

            // If the next date entry is a new date, calculate the average & outreach method for current date and save it to the HashMap
            if (simpleDate.compareTo(nextSimpleDate) != 0) {

                // save average temperature and weather condition
                int averageTemp = tempTotals / tempCount;
                String weather = "Not Sunny";
                if (sunny) {
                    weather = "Sunny";
                } else if (rainy) {
                    weather = "Rainy";
                }

                String bestOutreach = determineOutreachMethod(averageTemp, weather);
                forecast.put(simpleDate, bestOutreach);

                tempCount = 0;
                tempTotals = 0;
                rainy = false;
                sunny = false;
            }
        }

        return forecast;
    }

    /**
     * Prints out the Forecast Data.  Will print one line per date, each line with format "Date, Outreach".
     */
    public static void printForecast() {
        for (HashMap.Entry<String, String> entry : getForecast().entrySet()) {
            System.out.println(entry.getKey().substring(0, 10) + ", " + entry.getValue());
        }
    }

    /**
     * Determines the best outreach method given the daily temperature and weather outlook
     *
     * @param temp int Fahrenheit temperature
     * @param weather String describing weather, "Sunny" or "Rainy".
     * @return String of recommended outreach; "Text Message", "Email" or "Phone Call"
     */
    public static String determineOutreachMethod(int temp, String weather) {
        if (temp > 75 && weather == "Sunny") {
            return "Text Message";
        } else if (temp < 55 || weather == "Rainy") {
            return "Phone Call";
        } else if (temp < 75 && temp > 55) {
            return "Email";
        }

        // should never reach here, if we did something went wrong
        return "Unknown";
    }

    public static void main(String[] args) {
        printForecast();
    }
}