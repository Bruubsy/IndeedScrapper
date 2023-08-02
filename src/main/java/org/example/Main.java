package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter job title");
        String inputJobTitle = scanner.nextLine();
        System.out.println("Enter Location");
        String inputLocation = scanner.nextLine();

        inputJobTitle = inputJobTitle.toLowerCase();
        List<String> inputJobTitleWordList = Arrays.asList(inputJobTitle.split(" "));

        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(60000));
        driver.get("https://www.linkedin.com/");
        WebElement jobTitleTextBox = driver.findElement(By.name("keywords"));
        jobTitleTextBox.sendKeys(inputJobTitle);
        WebElement locationTextBox = driver.findElement(By.name("location"));
        locationTextBox.clear();
        locationTextBox.sendKeys(inputLocation);
        locationTextBox.sendKeys(Keys.RETURN);

        driver.findElement(By.xpath("//*[@id='jserp-filters']/ul/li/div/div/button")).click();
        String results = driver.findElement(By.xpath("//*[@id='jserp-filters']/ul/li/div/div/div/div/div/div/label")).getText().replaceAll(",","");
        int resultsNb = Integer.parseInt(results.substring(results.indexOf("(")+1, results.indexOf(")"))); //Cannot load more than 1000 results
        driver.findElement(By.xpath("//*[@id='jserp-filters']/ul/li/div/div/div/div/div/div/label")).click();
        driver.findElement(By.className("filter__submit-button")).click();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        while (!driver.findElement(By.cssSelector("#main-content > section.two-pane-serp-page__results-list > button")).isDisplayed()
        && !Objects.equals(driver.findElement(By.xpath("//*[@id=\"main-content\"]/section[2]/div[2]/p")).getText(), "You've viewed all jobs for this search")) {
            js.executeScript("window.scrollBy(0,document.body.scrollHeight)");
        }

        while (Integer.parseInt(driver.findElement(By.xpath("(//ul[@class='jobs-search__results-list']/li)[last()]/div")).getAttribute("data-row"))<Math.min(980,resultsNb-1)
                && !Objects.equals(driver.findElement(By.xpath("//*[@id=\"main-content\"]/section[2]/div[2]/p")).getText(), "You've viewed all jobs for this search")){
                WebElement button = driver.findElement(By.cssSelector("#main-content > section.two-pane-serp-page__results-list > button"));
                button.click();
        }


        //By.xpath("//ul[@class='jobs-search__results-list']/li/div/div[2]/h3") to get job name
        //By.xpath("//ul[@class='jobs-search__results-list']/li/div/div[2]/h4/a") to get company name
        // By.xpath("//ul[@class='jobs-search__results-list']/li/div/div[2]/div/span") to get location
        // By.xpath("//*[@id="main-content"]/section[2]/ul/li/div/a") to get link

        List<String> foundJobsTitleList = driver.findElements(By.xpath("//ul[@class='jobs-search__results-list']/li/div/div[2]/h3")).stream().map(WebElement::getText).collect(Collectors.toList());
        List<String> foundJobsCompanyNameList = driver.findElements(By.xpath("//ul[@class='jobs-search__results-list']/li/div/div[2]/h4/a")).stream().map(WebElement::getText).collect(Collectors.toList());
        List<String> foundJobsLink = driver.findElements(By.xpath("//*[@id='main-content']/section[2]/ul/li/div/a")).stream().map(str -> str.getAttribute("href")).collect(Collectors.toList());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("results.csv"), StandardCharsets.UTF_8))) {
            writer.write("\uFEFF");
            writer.write("Job,Company,Link\n");
            for (int i = 0; i < foundJobsTitleList.size(); i++) {
                List<String> foundJobTitleWordList = Arrays.asList(foundJobsTitleList.get(i).split(" "));
                if (foundJobTitleWordList.stream().map(String::toLowerCase).anyMatch(inputJobTitleWordList::contains)){
                    writer.write(foundJobsTitleList.get(i).replaceAll(","," ") + "," + foundJobsCompanyNameList.get(i).replaceAll(","," ") + "," + foundJobsLink.get(i) + "\n");
                }
            }
            System.out.println("Data written to CSV file successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        driver.close();
    }
}
